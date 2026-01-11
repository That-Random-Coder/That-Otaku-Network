package com.project.content_service.domain.entity;

import com.project.content_service.domain.enums.LikeOrDislikeEnums;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Table
@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeShare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content")
    private Content content;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LikeOrDislikeEnums likeOrDislike;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private LocalDateTime likeAndDislikeAt;

    @PreUpdate
    private void atCreate() {
        this.likeAndDislikeAt = LocalDateTime.now();
    }

}
