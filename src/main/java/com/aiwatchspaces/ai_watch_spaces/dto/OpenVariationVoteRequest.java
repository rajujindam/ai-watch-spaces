package com.aiwatchspaces.ai_watch_spaces.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OpenVariationVoteRequest(

        @NotNull(message = "Duration is required")
        @Min(value = 5, message = "Vote duration must be at least 5 seconds")
        @Max(value = 300, message = "Vote duration cannot exceed 300 seconds")
        Integer durationSeconds

) {
}