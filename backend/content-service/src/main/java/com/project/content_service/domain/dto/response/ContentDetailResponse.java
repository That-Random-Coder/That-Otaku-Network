package com.project.content_service.domain.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContentDetailResponse {

    private UUID id;

    private String title;

    private String bio;

    private UUID userID;

    private String userName;

    private String displayName;

    private byte[] media;

    private String mediaType;

    private Long likeCount;

    private Long dislikeCount;

    private Long commentCount;

    private Long shareCount;

    private Boolean isLiked;

    private Boolean isDisliked;

    private LocalDateTime created;

    public ContentDetailResponse(UUID id, String title, String bio, UUID userID, String userName,
            String displayName, Long likeCount, Long dislikeCount, Long commentCount, Long shareCount,
            Boolean isLiked, Boolean isDisliked, LocalDateTime created) {
        this.id = id;
        this.title = title;
        this.bio = bio;
        this.userID = userID;
        this.userName = userName;
        this.displayName = displayName;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.commentCount = commentCount;
        this.shareCount = shareCount;
        this.isLiked = isLiked;
        this.isDisliked = isDisliked;
        this.created = created;
    }
}
