package com.project.recommendation_service.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.swing.text.html.HTML.Tag;

@Table
@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Content {

    @Id
    private UUID contentId;

    @Column(nullable = false)
    private String contentTitle;

    @Column(nullable = false)
    private String username;

    @BatchSize(size = 10)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "content_genres", joinColumns = @JoinColumn(name = "content_id"))
    @Column(name = "genre", nullable = false)
    private Set<String> genre;

    @BatchSize(size = 10)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "content_categories", joinColumns = @JoinColumn(name = "content_id"))
    @Column(name = "category", nullable = false)
    private Set<String> category;

    @Column(nullable = false)
    private LocalDateTime timeOfCreation;

    private long likeCount;

    private long dislikeCount;

    private long commentCount;

    private long shareCount;

    private Boolean enable;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "content")
    private Set<UsersInteraction> userInteraction = new HashSet<>();

    @BatchSize(size = 10)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "content_tags", joinColumns = @JoinColumn(name = "content_id"))
    @Column(name = "tag", nullable = false)
    private Set<String> contentTag;

    @PrePersist
    private void atStart() {
        this.enable = true;
    }
}
