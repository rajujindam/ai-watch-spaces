package com.aiwatchspaces.ai_watch_spaces.dto;

public record VariationVoteResult(
        Long variationId,
        Long winningOptionId,
        Integer timestampSeconds,
        String assetRef
) {
}