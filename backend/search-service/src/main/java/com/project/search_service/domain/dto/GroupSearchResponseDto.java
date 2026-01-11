package com.project.search_service.domain.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSearchResponseDto {
    private String id;
    private String groupName;
    private String bio;
    private String leaderUsername;
    private String leaderDisplayName;
}
