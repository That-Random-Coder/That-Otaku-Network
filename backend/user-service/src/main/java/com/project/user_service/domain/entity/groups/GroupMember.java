package com.project.user_service.domain.entity.groups;

import com.project.user_service.domain.entity.users.Users;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.util.UUID;

@Table(
        uniqueConstraints = @UniqueConstraint(
                name = "unique_members" ,
                columnNames = {"users_id", "group_id"}
        )
)
@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id" , nullable = false)
    private Users users;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id" , nullable = false)
    private Group group;

    @CreatedDate
    @Column(nullable = false , updatable = false)
    private LocalDate joinDate;

    @PrePersist
    private void atCreation(){
        joinDate = LocalDate.now();
    }
}
