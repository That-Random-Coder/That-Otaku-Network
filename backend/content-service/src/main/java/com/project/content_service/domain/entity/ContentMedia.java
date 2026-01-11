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
public class ContentMedia {

    @Id
    private UUID id;

    @Lob
    @Column(nullable = false , updatable = false)
    private byte[] content;

    @Column(nullable = false)
    private String contentType;

    private LocalDateTime updateImageTimeStamp;

    @PreUpdate
    private void atCreate() {
        this.updateImageTimeStamp = LocalDateTime.now();
    }
}
