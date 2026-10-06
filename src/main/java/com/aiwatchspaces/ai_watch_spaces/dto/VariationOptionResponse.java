package com.aiwatchspaces.ai_watch_spaces.dto;

public record VariationOptionResponse(
        Long id,
        Long timelineEventId,
        String label,
        String assetRef,
        Integer voteCount
) {
}