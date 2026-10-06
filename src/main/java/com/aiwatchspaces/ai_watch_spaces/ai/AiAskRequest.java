package com.aiwatchspaces.ai_watch_spaces.ai;

import jakarta.validation.constraints.NotBlank;

public record AiAskRequest(
        @NotBlank(message = "Question cannot be blank")
        String question
) {
}