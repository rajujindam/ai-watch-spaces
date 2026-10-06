package com.aiwatchspaces.ai_watch_spaces.dto;

import java.time.Instant;
import java.util.List;

public record VariationVoteResponse(
        Long variationId,
        List<VariationOptionResponse> options,
        Instant closesAt
) {
}