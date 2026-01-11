package com.project.search_service.domain.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchUserDto {

    private UUID id;

    private String username;

    private String displayName;

    private String bio;
}
