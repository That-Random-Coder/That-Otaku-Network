package com.project.auth_service.domain.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class LoginResponseDto {
    private UUID id;
    private String username;
    private String email;
    private String AccessToken;
    private Date TimeStampAccessToken;
    private String RefreshToken;
    private Date TimeStampRefreshToken;
}
