package com.project.recommendation_service.service;

import com.project.recommendation_service.domain.dto.FirstRecommendationRequest;
import com.project.recommendation_service.domain.dto.RecommendationResult;
import com.project.recommendation_service.exception.ContentNotFoundException;
import com.project.recommendation_service.repository.InteractRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RecommendationService {

    private final InteractRepository interactRepository;

    @Transactional
    public Set<RecommendationResult> getRecommendation(UUID id){
        Set<RecommendationResult> results = interactRepository.getRecommendation(id);

        if(!results.isEmpty()){
            return results;
        }else{
            throw new ContentNotFoundException("Content not found");
        }
    }

    @Transactional
    public Set<RecommendationResult> getFirstRecommendation(FirstRecommendationRequest request){
        if(request.getCategory() == null || request.getCategory().isEmpty() || request.getGenre() == null || request.getGenre().isEmpty()){
            throw new IllegalArgumentException("Illegal Argument");
        }

        Set<RecommendationResult> results = interactRepository.getRecommendationByPreferences(request.getCategory() , request.getGenre());

        if(!results.isEmpty()){
            return results;
        }else{
            throw new ContentNotFoundException("Content not found");
        }

    }
}
