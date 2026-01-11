package com.project.user_service.domain.entity.groups;

import com.project.user_service.domain.entity.users.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Table(name = "groups", uniqueConstraints = @UniqueConstraint(name = "group_name_unique", columnNames = "group_name"))
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_name", nullable = false, updatable = false)
    private String groupName;

    @Column(nullable = false)
    private String bio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Users leader;

    @Builder.Default
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "group")
    private Set<GroupMember> members = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDate dateOfCreation;

    @OneToOne(fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "group")
    private ImageGroup images; 

    private Boolean enable;

    @PrePersist
    private void atCreation() {
        dateOfCreation = LocalDate.now();
        enable = true;
    }
}
