package com.project.user_service.domain.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponseDto {

    private UUID id;

    private String username;

    private String displayName;

    private String bio;

    private long followers;

    private long following;

    private String location;

    private byte[] profileImg;

    private String imageType;

    private Boolean isVerified;

    private Boolean isFollow;
}
