package com.aiwatchspaces.ai_watch_spaces.dto;

import jakarta.validation.constraints.NotNull;

public record CastVariationVoteRequest(

        @NotNull(message = "Option ID is required")
        Long optionId

) {
}