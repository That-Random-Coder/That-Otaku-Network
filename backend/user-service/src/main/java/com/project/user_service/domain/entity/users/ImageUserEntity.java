package com.project.user_service.domain.entity.users;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUserEntity {

    @Id
    private UUID id;

    @Lob
    private byte[] image;

    private String imageType;

}
