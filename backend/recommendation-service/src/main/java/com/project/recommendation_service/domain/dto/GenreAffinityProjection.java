package com.project.recommendation_service.domain.dto;

public interface GenreAffinityProjection {

    String getGenre();

    Double getRawScore();

    Double getAffinityWeight();

}
