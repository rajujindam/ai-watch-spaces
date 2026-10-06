package com.aiwatchspaces.ai_watch_spaces.dto;

public record RecommendationResponse(
        Long titleId,
        String title,
        double contentScore,
        double collaborativeScore,
        double hybridScore
) {
}