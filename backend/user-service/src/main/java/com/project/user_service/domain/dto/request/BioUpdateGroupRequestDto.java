package com.project.user_service.domain.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BioUpdateGroupRequestDto {
    private UUID id;
    private String bio;
}
