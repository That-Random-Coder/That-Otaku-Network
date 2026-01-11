package com.project.user_service.domain.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetFollowResponse {
    private UUID id;
    private String displayName;
    private String username;
    private boolean isValidated;
}
