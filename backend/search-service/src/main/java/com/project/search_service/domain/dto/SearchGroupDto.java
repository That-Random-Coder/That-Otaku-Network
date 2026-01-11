package com.project.search_service.domain.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchGroupDto {

    private String id;
    private String groupUsername;
    private String bio;
    private String leaderUsername;
    private String leaderDisplayName;

}
