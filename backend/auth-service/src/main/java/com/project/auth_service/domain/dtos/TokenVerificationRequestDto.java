package com.project.auth_service.domain.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenVerificationRequestDto {

    @NonNull
    private UUID id;

    @NonNull
    private int code;

}
