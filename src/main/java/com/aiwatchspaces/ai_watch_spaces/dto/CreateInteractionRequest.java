package com.aiwatchspaces.ai_watch_spaces.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateInteractionRequest(

        @NotNull
        Long titleId,

        @NotNull
        @Min(0)
        Integer watchedSeconds,

        @NotNull
        Boolean completed,

        @Min(1)
        @Max(5)
        Integer rating
) {
}