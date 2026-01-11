package com.project.auth_service.domain.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class SignUpResponseDto {

    private UUID id;
    private String username;
    private String email;
    private boolean emailSend;

}

