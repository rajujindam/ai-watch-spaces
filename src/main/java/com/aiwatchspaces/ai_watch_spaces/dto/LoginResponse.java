package com.aiwatchspaces.ai_watch_spaces.dto;

public record LoginResponse(
        String token,
        String refreshToken
) {
}