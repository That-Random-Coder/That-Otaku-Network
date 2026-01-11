package com.project.user_service.domain.entity.groups;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Table
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageGroup {
    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "group_id")
    private Group group;

    @Lob
    @Column(name = "profile_image", nullable = true)
    private byte[] profileImage;

    private String profileImageType;

    @Lob
    @Column(name = "bg_image", nullable = true)
    private byte[] bgImage;

    private String bgImageType;
}
