package com.project.user_service.domain.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetGroups {
    private UUID id;
    private String groupName;
    private String leaderUsername;
    private UUID leaderId;
}
