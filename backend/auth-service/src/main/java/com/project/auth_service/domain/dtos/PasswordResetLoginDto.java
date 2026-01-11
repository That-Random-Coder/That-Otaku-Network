package com.project.auth_service.domain.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetLoginDto {

    @NotNull
    private String identifier;

    @NotNull
    private int verificationCode;

    @NotBlank
    private String password;

}
