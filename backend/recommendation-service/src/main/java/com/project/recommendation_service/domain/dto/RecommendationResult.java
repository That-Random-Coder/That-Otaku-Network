package com.project.recommendation_service.domain.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RecommendationResult {
    UUID getContentId();
    String getContentTitle();
    String getUsername();
    Long getLikeCount();
    Long getDislikeCount();
    Long getCommentCount();
    LocalDateTime getTimeOfCreation();
}

