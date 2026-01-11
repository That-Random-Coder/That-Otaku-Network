package com.project.content_service.repository;

import com.project.content_service.domain.dto.response.ContentDetailResponse;
import com.project.content_service.domain.entity.Content;
import com.project.content_service.domain.entity.ContentMedia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID> {

    Optional<Content> findByIdAndEnableTrue(UUID id);

    Page<Content> findByUserIDAndEnableTrue(UUID userId, Pageable pageable);

    Page<Content> findByUserID(UUID userId, Pageable pageable);

    boolean existsByIdAndEnableTrue(UUID id);

    boolean existsByIdAndUserID(UUID id, UUID userID);

    @Modifying
    @Query("DELETE FROM Content c WHERE c.id = :id")
    int deleteByContentId(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Content c SET c.enable = false WHERE c.id = :id")
    int disableByContentId(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Content c SET c.enable = true WHERE c.id = :id")
    int enableByContentId(@Param("id") UUID id);

    @Query("""
                SELECT new com.project.content_service.domain.dto.response.ContentDetailResponse(
                    c.id,
                    c.title,
                    c.bio,
                    c.userID,
                    c.userName,
                    c.displayName,
                    (SELECT COUNT(ls) FROM LikeShare ls WHERE ls.content.id = c.id AND ls.likeOrDislike = 'LIKE'),
                    (SELECT COUNT(ls) FROM LikeShare ls WHERE ls.content.id = c.id AND ls.likeOrDislike = 'DISLIKE'),
                    (SELECT COUNT(cm) FROM Comments cm WHERE cm.content.id = c.id),
                    (SELECT COUNT(s) FROM Share s WHERE s.content.id = c.id),
                    (SELECT CASE WHEN COUNT(ls) > 0 THEN true ELSE false END FROM LikeShare ls WHERE ls.content.id = c.id AND ls.userId = :currentUserId AND ls.likeOrDislike = 'LIKE'),
                    (SELECT CASE WHEN COUNT(ls) > 0 THEN true ELSE false END FROM LikeShare ls WHERE ls.content.id = c.id AND ls.userId = :currentUserId AND ls.likeOrDislike = 'DISLIKE'),
                    c.created
                )
                FROM Content c
                WHERE c.id = :id AND c.enable = true
            """)
    Optional<ContentDetailResponse> getContentDetailById(@Param("id") UUID id,
            @Param("currentUserId") UUID currentUserId);

    @Query("""
                SELECT new com.project.content_service.domain.dto.response.ContentDetailResponse(
                    c.id,
                    c.title,
                    c.bio,
                    c.userID,
                    c.userName,
                    c.displayName,
                    (SELECT COUNT(ls) FROM LikeShare ls WHERE ls.content.id = c.id AND ls.likeOrDislike = 'LIKE'),
                    (SELECT COUNT(ls) FROM LikeShare ls WHERE ls.content.id = c.id AND ls.likeOrDislike = 'DISLIKE'),
                    (SELECT COUNT(cm) FROM Comments cm WHERE cm.content.id = c.id),
                    (SELECT COUNT(s) FROM Share s WHERE s.content.id = c.id),
                    (SELECT CASE WHEN COUNT(ls) > 0 THEN true ELSE false END FROM LikeShare ls WHERE ls.content.id = c.id AND ls.userId = :currentUserId AND ls.likeOrDislike = 'LIKE'),
                    (SELECT CASE WHEN COUNT(ls) > 0 THEN true ELSE false END FROM LikeShare ls WHERE ls.content.id = c.id AND ls.userId = :currentUserId AND ls.likeOrDislike = 'DISLIKE'),
                    c.created
                )
                FROM Content c
                WHERE c.id IN :ids AND c.enable = true
            """)
    Page<ContentDetailResponse> getContentDetailsByIds(@Param("ids") List<UUID> ids,
            @Param("currentUserId") UUID currentUserId, Pageable pageable);

    @Query("SELECT cm FROM ContentMedia cm WHERE cm.id = :id")
    Optional<ContentMedia> getMediaById(@Param("id") UUID id);

    @Query("SELECT cm FROM ContentMedia cm WHERE cm.id IN :ids")
    List<ContentMedia> getMediaByIds(@Param("ids") List<UUID> ids);

    @Query(value = """
                SELECT new com.project.content_service.domain.dto.response.ContentDetailResponse(
                    c.id,
                    c.title,
                    c.bio,
                    c.userID,
                    c.userName,
                    c.displayName,
                    (SELECT COUNT(ls) FROM LikeShare ls WHERE ls.content.id = c.id AND ls.likeOrDislike = 'LIKE'),
                    (SELECT COUNT(ls) FROM LikeShare ls WHERE ls.content.id = c.id AND ls.likeOrDislike = 'DISLIKE'),
                    (SELECT COUNT(cm) FROM Comments cm WHERE cm.content.id = c.id),
                    (SELECT COUNT(s) FROM Share s WHERE s.content.id = c.id),
                    (SELECT CASE WHEN COUNT(ls) > 0 THEN true ELSE false END FROM LikeShare ls WHERE ls.content.id = c.id AND ls.userId = :currentUserId AND ls.likeOrDislike = 'LIKE'),
                    (SELECT CASE WHEN COUNT(ls) > 0 THEN true ELSE false END FROM LikeShare ls WHERE ls.content.id = c.id AND ls.userId = :currentUserId AND ls.likeOrDislike = 'DISLIKE'),
                    c.created
                )
                FROM Content c
                WHERE c.userID = :userId AND c.enable = true
                ORDER BY c.created DESC
            """)
    Page<ContentDetailResponse> getContentDetailsByUserId(@Param("userId") UUID userId,
            @Param("currentUserId") UUID currentUserId, Pageable pageable);

    @Query(value = """
                SELECT new com.project.content_service.domain.dto.response.ContentDetailResponse(
                    c.id,
                    c.title,
                    c.bio,
                    c.userID,
                    c.userName,
                    c.displayName,
                    (SELECT COUNT(ls) FROM LikeShare ls WHERE ls.content.id = c.id AND ls.likeOrDislike = 'LIKE'),
                    (SELECT COUNT(ls) FROM LikeShare ls WHERE ls.content.id = c.id AND ls.likeOrDislike = 'DISLIKE'),
                    (SELECT COUNT(cm) FROM Comments cm WHERE cm.content.id = c.id),
                    (SELECT COUNT(s) FROM Share s WHERE s.content.id = c.id),
                    (SELECT CASE WHEN COUNT(ls) > 0 THEN true ELSE false END FROM LikeShare ls WHERE ls.content.id = c.id AND ls.userId = :currentUserId AND ls.likeOrDislike = 'LIKE'),
                    (SELECT CASE WHEN COUNT(ls) > 0 THEN true ELSE false END FROM LikeShare ls WHERE ls.content.id = c.id AND ls.userId = :currentUserId AND ls.likeOrDislike = 'DISLIKE'),
                    c.created
                )
                FROM Content c
                WHERE c.userID IN :userIds AND c.enable = true
                ORDER BY c.created DESC
            """)
    Page<ContentDetailResponse> getContentDetailsByUserIds(@Param("userIds") List<UUID> userIds,
                                                          @Param("currentUserId") UUID currentUserId, Pageable pageable);

    @Query("SELECT cm.id FROM Content cm WHERE cm.userID = :userId AND cm.enable = true ORDER BY cm.created DESC")
    List<UUID> getContentIdsByUserId(@Param("userId") UUID userId, Pageable pageable);
}
