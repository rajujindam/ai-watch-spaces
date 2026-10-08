package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpaceEvent;
import com.aiwatchspaces.ai_watch_spaces.repository.WatchSpaceEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionAnalyticsService {

    private final WatchSpaceEventRepository watchSpaceEventRepository;

    public SessionAnalytics calculateAnalytics(Long watchSpaceId) {

        List<WatchSpaceEvent> events =
                watchSpaceEventRepository
                        .findByWatchSpaceIdOrderByOccurredAtAsc(watchSpaceId);

        if (events.isEmpty()) {
            return new SessionAnalytics(
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        Instant startTime = events.get(0).getOccurredAt();
        Instant endTime = events.get(events.size() - 1).getOccurredAt();

        long durationSeconds =
                Duration.between(startTime, endTime).getSeconds();

        int currentParticipants = 0;
        int peakParticipants = 0;

        int aiQuestions = 0;
        int triviaCards = 0;
        int chatMessages = 0;

        for (WatchSpaceEvent event : events) {

            switch (event.getEventType()) {

                case "PARTICIPANT_JOINED" ->
                        currentParticipants++;

                case "PARTICIPANT_LEFT" ->
                        currentParticipants--;

                case "AI_QUESTION" ->
                        aiQuestions++;

                case "TRIVIA_SERVED" ->
                        triviaCards++;

                case "CHAT_MESSAGE" ->
                        chatMessages++;
            }

            peakParticipants =
                    Math.max(
                            peakParticipants,
                            currentParticipants
                    );
        }

        return new SessionAnalytics(
                durationSeconds,
                peakParticipants,
                aiQuestions,
                triviaCards,
                chatMessages
        );
    }

    public record SessionAnalytics(
            long durationSeconds,
            int peakConcurrentParticipants,
            int aiQuestions,
            int triviaCards,
            int chatMessages
    ) {
    }
}