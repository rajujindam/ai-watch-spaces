package com.aiwatchspaces.ai_watch_spaces.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecommendationEvaluator {

    private final RecommendationService recommendationService;

    public double calculatePrecisionAt5(
            Long userId,
            Set<Long> heldOutTitleIds
    ) {
        List<RecommendationService.RecommendationScore> recommendations =
                recommendationService.recommend(userId, 5);

        long relevantRecommendations =
                recommendations.stream()
                        .map(recommendation ->
                                recommendation.title().getId())
                        .filter(heldOutTitleIds::contains)
                        .count();

        return (double) relevantRecommendations / 5;
    }
}