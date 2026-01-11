package com.project.content_service.domain.entity;

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
public class Comments {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Content content;

    @Column(nullable = false)
    private String comment;

    @Column(nullable = false)
    private String userUserName;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private LocalDateTime commentAt;

    @PreUpdate
    private void atCreate() {
        commentAt = LocalDateTime.now();
    }
}
