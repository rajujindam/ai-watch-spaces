package com.aiwatchspaces.ai_watch_spaces.dto;

import java.time.Instant;

public record InteractionResponse(
        Long id,
        Long userId,
        Long titleId,
        Integer watchedSeconds,
        Boolean completed,
        Integer rating,
        Instant createdAt
) {
}