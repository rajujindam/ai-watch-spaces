package com.aiwatchspaces.ai_watch_spaces.controller;

import com.aiwatchspaces.ai_watch_spaces.dto.RecommendationResponse;
import com.aiwatchspaces.ai_watch_spaces.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public Mono<List<RecommendationResponse>> getRecommendations(
            @RequestParam(defaultValue = "5") int limit,
            JwtAuthenticationToken authentication
    ) {
        Long userId = Long.parseLong(
                authentication.getToken().getSubject()
        );

        return Mono.fromCallable(() ->
                        recommendationService.recommend(
                                userId,
                                limit
                        ))
                .subscribeOn(Schedulers.boundedElastic())
                .map(recommendations ->
                        recommendations.stream()
                                .map(recommendation ->
                                        new RecommendationResponse(
                                                recommendation.title().getId(),
                                                recommendation.title().getName(),
                                                recommendation.contentScore(),
                                                recommendation.collaborativeScore(),
                                                recommendation.hybridScore()
                                        ))
                                .toList()
                );
    }
}