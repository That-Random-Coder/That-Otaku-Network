package com.project.user_service.domain.entity.users;

import com.project.user_service.domain.entity.groups.GroupMember;
import com.project.user_service.domain.entity.groups.Group;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Table
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Users {

    @Id
    @Column(updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(length = 50, nullable = false)
    private String displayName;

    @Column(nullable = false, length = 200)
    private String bio;

    @Column(nullable = false, length = 50)
    private String location;

    @Column(nullable = false)
    private LocalDate dateOfBirth;


    @Builder.Default
    @OneToMany(mappedBy = "following")
    private Set<Follow> followers = new HashSet<>();


    @Builder.Default
    @OneToMany(mappedBy = "follower")
    private Set<Follow> following = new HashSet<>();

    @OneToOne(cascade = CascadeType.ALL , orphanRemoval = true , fetch = FetchType.LAZY)
    private ImageUserEntity imageUserEntity;

    @Column(nullable = false)
    private boolean enable;

    @Column(nullable = false)
    private boolean isVerified = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(orphanRemoval = true , mappedBy = "leader" , cascade = CascadeType.ALL , fetch = FetchType.LAZY)
    private Set<Group> leaderOfGroup = new HashSet<>();

    @Builder.Default
    @OneToMany(orphanRemoval = true , mappedBy = "users" , cascade = CascadeType.ALL , fetch = FetchType.LAZY)
    private Set<GroupMember> groupMembers = new HashSet<>();

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.enable = true;
        this.isVerified = false;
    }
}
