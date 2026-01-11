package com.project.user_service.domain.dto.response;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponseDto {

    private UUID id;
    private String groupName;
    private String groupBio;
    private String leaderUsername;
    private UUID leaderId;
    private String leaderDisplayName;
    private long memberCount;
    private LocalDate dateOfCreation;
    private Boolean isMember;
    private byte[] profileImage;
    private String profileImageType;
    private byte[] bgImage;
    private String bgImageType;
}
