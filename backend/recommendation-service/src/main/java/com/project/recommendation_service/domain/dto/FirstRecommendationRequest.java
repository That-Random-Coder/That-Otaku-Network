package com.project.recommendation_service.domain.dto;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FirstRecommendationRequest {

    private Set<String> category;
    private Set<String> genre;
}
