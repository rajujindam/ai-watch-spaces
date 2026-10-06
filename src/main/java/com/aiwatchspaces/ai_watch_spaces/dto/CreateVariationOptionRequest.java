package com.aiwatchspaces.ai_watch_spaces.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateVariationOptionRequest(

        @NotBlank(message = "Label cannot be blank")
        String label,

        String assetRef
) {
}