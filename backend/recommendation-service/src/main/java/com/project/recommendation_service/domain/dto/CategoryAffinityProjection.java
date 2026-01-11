package com.project.recommendation_service.domain.dto;

public interface CategoryAffinityProjection {

    String getCategory();

    Double getRawScore();

    Double getAffinityWeight();
}
