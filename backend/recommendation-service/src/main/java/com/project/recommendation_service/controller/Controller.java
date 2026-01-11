package com.project.recommendation_service.controller;

import com.project.recommendation_service.domain.dto.FirstRecommendationRequest;
import com.project.recommendation_service.domain.dto.RecommendationResult;
import com.project.recommendation_service.service.RecommendationService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/feed")
@AllArgsConstructor
public class Controller {

    private final RecommendationService recommendationService;

    @GetMapping("/get")
    @PreAuthorize("authentication.principal.id.equals(#id)")
    public ResponseEntity<Set<RecommendationResult>> getFeed(
            @RequestParam("userId")UUID id
    ){
        return new ResponseEntity<>(recommendationService.getRecommendation(id) ,HttpStatus.OK);
    }

    @PostMapping("/first/get")
    public ResponseEntity<Set<RecommendationResult>> getFirstFeed(
            @RequestBody FirstRecommendationRequest request
            ){
        return new ResponseEntity<>(recommendationService.getFirstRecommendation(request),HttpStatus.OK);
    }
}
