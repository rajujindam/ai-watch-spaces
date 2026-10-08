package com.aiwatchspaces.ai_watch_spaces.dto;

public record SessionAnalyticsResponse(
        long durationSeconds,
        int peakConcurrentParticipants,
        int aiQuestions,
        int triviaCards,
        int chatMessages
) {
}