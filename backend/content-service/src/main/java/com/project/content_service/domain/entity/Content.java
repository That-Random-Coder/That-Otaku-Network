package com.project.content_service.domain.entity;

import com.project.content_service.domain.enums.Category;
import com.project.content_service.domain.enums.Genre;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Table
@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Category> animeCategories;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Genre> genre;

    private Set<String> tags;

    private String bio;

    @Column(nullable = false)
    private UUID userID;

    @Column(nullable = false)
    private String userName;

    @Column(nullable = false)
    private String displayName;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private ContentMedia contentMedia;

    @OneToMany(mappedBy = "content", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<LikeShare> likeAndDislikes;

    @OneToMany(mappedBy = "content", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<Comments> comments;

    @OneToMany(mappedBy = "content", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<Share> shares;

    @Column(nullable = false)
    private Boolean enable;

    @Column(nullable = false)
    private LocalDateTime created;

    @PrePersist
    private void atCreate() {
        this.enable = true;
        this.created = LocalDateTime.now();
    }
}
