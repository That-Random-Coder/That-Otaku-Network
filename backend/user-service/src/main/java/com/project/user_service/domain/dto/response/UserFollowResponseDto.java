package com.project.user_service.domain.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFollowResponseDto {

    private UUID id;
    private String username;

}
