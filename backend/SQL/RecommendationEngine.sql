WITH constants AS (
    SELECT
		2    ::int AS liked_power,
		4    ::int AS comment_power,
		-5   ::int AS dislike_power,
	
        0.5  ::numeric AS category_factor,
        1.0  ::numeric AS genre_factor,
        0.8  ::numeric AS tag_factor,

        20.0 ::numeric AS interaction_index_half_life,
        45.0 ::numeric AS interaction_time_half_life,
        30.0 ::numeric AS content_time_half_life,

        0.4  ::numeric AS mean_score_weight
),

ordered_interactions AS (
    SELECT
        ui.user_id,
        ui.content_content_id,
        ui.interact_at,
        CASE
            WHEN ui.is_liked     THEN  c.liked_power
            WHEN ui.is_commented THEN  c.comment_power
            WHEN ui.is_disliked  THEN  c.dislike_power
            ELSE 0
        END AS action_weight,
        ROW_NUMBER() OVER (
            PARTITION BY ui.user_id
            ORDER BY ui.interact_at DESC
        ) - 1 AS interaction_index
    FROM users_interaction ui
	CROSS JOIN constants c
    WHERE ui.user_id = '1b3d1f03-eab5-433f-82bd-a89946b41a16'
),

interaction_scores AS (
    SELECT
        oi.content_content_id,
        oi.action_weight
        * EXP(-LN(2) * oi.interaction_index / c.interaction_index_half_life)
        * EXP(-LN(2) * EXTRACT(DAY FROM (NOW() - oi.interact_at)) / c.interaction_time_half_life)
        AS score
    FROM ordered_interactions oi
    CROSS JOIN constants c
),

category_stats AS (
    SELECT
        cc.category,
        SUM(iscore.score) / SQRT(COUNT(*)) AS raw_category_score
    FROM interaction_scores iscore
    JOIN content_categories cc
        ON cc.content_id = iscore.content_content_id
    GROUP BY cc.category
),

category_affinity AS (
    SELECT
        category,
        EXP(raw_category_score)
        / SUM(EXP(raw_category_score)) OVER () AS category_weight
    FROM category_stats
),

genre_stats AS (
    SELECT
        cg.genre,
        SUM(iscore.score) / SQRT(COUNT(*)) AS raw_genre_score
    FROM interaction_scores iscore
    JOIN content_genres cg
        ON cg.content_id = iscore.content_content_id
    GROUP BY cg.genre
),

genre_affinity AS (
    SELECT
        genre,
        EXP(raw_genre_score)
        / SUM(EXP(raw_genre_score)) OVER () AS genre_weight
    FROM genre_stats
),

tag_stats AS (
    SELECT
        tg.tag,
        SUM(iscore.score) / SQRT(COUNT(*)) AS raw_tag_score
    FROM interaction_scores iscore
    JOIN content_tags tg
        ON tg.content_id = iscore.content_content_id
    GROUP BY tg.tag
),

tag_affinity AS (
    SELECT
        tag,
        EXP(raw_tag_score)
        / SUM(EXP(raw_tag_score)) OVER () AS tag_weight
    FROM tag_stats
),

raw_content_scores AS (
    SELECT
        c.content_id,
        c.content_title,
        (
            cns.mean_score_weight
            * (COALESCE(c.like_count,0) + COALESCE(c.comment_count,0))::numeric
            / NULLIF(c.like_count + c.dislike_count + c.comment_count, 1)
            + 1
        ) AS mean_score,
        c.time_of_creation
    FROM content c
    CROSS JOIN constants cns
    WHERE NOT EXISTS (
        SELECT 1
        FROM users_interaction ui
        WHERE ui.content_content_id = c.content_id
          AND ui.user_id = '1b3d1f03-eab5-433f-82bd-a89946b41a16'
          AND ui.interact_at >= NOW() - INTERVAL '30 days'
    )
),

preference_treatment_scores AS (
    SELECT
        rcs.content_id,
        rcs.content_title,
        rcs.mean_score,
        rcs.time_of_creation,

        COALESCE(SUM(DISTINCT ca.category_weight), 0) AS category_multiplier,
        COALESCE(SUM(DISTINCT ga.genre_weight), 0)    AS genre_multiplier,
        COALESCE(SUM(DISTINCT ta.tag_weight), 0)      AS tag_multiplier,

        (rcs.mean_score + 1) *
        (
            1
            + c.category_factor * COALESCE(SUM(DISTINCT ca.category_weight), 0)
            + c.genre_factor    * COALESCE(SUM(DISTINCT ga.genre_weight), 0)
            + c.tag_factor      * COALESCE(SUM(DISTINCT ta.tag_weight), 0)
        ) AS weighted_score

    FROM raw_content_scores rcs
    CROSS JOIN constants c

    LEFT JOIN content_categories cc
        ON cc.content_id = rcs.content_id
    LEFT JOIN category_affinity ca
        ON ca.category = cc.category

    LEFT JOIN content_genres cg
        ON cg.content_id = rcs.content_id
    LEFT JOIN genre_affinity ga
        ON ga.genre = cg.genre

    LEFT JOIN content_tags ct
        ON ct.content_id = rcs.content_id
    LEFT JOIN tag_affinity ta
        ON ta.tag = ct.tag

    GROUP BY
        rcs.content_id,
        rcs.content_title,
        rcs.mean_score,
        rcs.time_of_creation,
        c.category_factor,
        c.genre_factor,
        c.tag_factor
),

decay_time_score AS (
    SELECT
        pts.*,
        EXTRACT(DAY FROM (NOW() - pts.time_of_creation))::int AS days_ago,
        pts.weighted_score
        * EXP(-LN(2) * EXTRACT(DAY FROM (NOW() - pts.time_of_creation)) / c.content_time_half_life)
        AS final_score
    FROM preference_treatment_scores pts
    CROSS JOIN constants c
)

SELECT *
FROM decay_time_score
ORDER BY final_score DESC;
