package com.project.user_service.domain.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDetailResponseDto {

    private UUID id;
    private String groupName;
    private String groupBio;

    private byte[] groupProfileImg;
    private byte[] groupBackgroundImg;
}
