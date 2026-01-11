package com.project.recommendation_service.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(
        name = "users_interaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_content",
                columnNames = {"user_id", "content_id"}
        )
)
@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsersInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    private Content content;

    @Column(name = "is_liked")
    private Boolean like;

    @Column(name = "is_disliked")
    private Boolean dislike;

    @Column(name = "is_commented")
    private Boolean comment;

    @Column(name = "is_shared")
    private Boolean share;

    @Column
    private LocalDateTime interactAt;

}
