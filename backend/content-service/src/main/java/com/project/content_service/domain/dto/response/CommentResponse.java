package com.project.content_service.domain.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentResponse {

    private UUID id;

    private String comment;

    private UUID userId;

    private String userName;

    private String displayName;

    private LocalDateTime commentAt;

    private String timeAgo;

    public CommentResponse(UUID id, String comment, UUID userId, String userName, String displayName,
            LocalDateTime commentAt) {
        this.id = id;
        this.comment = comment;
        this.userId = userId;
        this.userName = userName;
        this.displayName = displayName;
        this.commentAt = commentAt;
        this.timeAgo = calculateTimeAgo(commentAt);
    }

    private String calculateTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }

        LocalDateTime now = LocalDateTime.now();
        long years = ChronoUnit.YEARS.between(dateTime, now);
        if (years > 0) {
            return years + (years == 1 ? " year ago" : " years ago");
        }

        long months = ChronoUnit.MONTHS.between(dateTime, now);
        if (months > 0) {
            return months + (months == 1 ? " month ago" : " months ago");
        }

        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days > 0) {
            return days + (days == 1 ? " day ago" : " days ago");
        }

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours > 0) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }

        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        if (minutes > 0) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }

        return "just now";
    }
}
