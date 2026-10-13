package com.aiwatchspaces.ai_watch_spaces.controller;

import com.aiwatchspaces.ai_watch_spaces.dto.SessionAnalyticsResponse;
import com.aiwatchspaces.ai_watch_spaces.service.SessionAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/watch-spaces")
@RequiredArgsConstructor
public class SessionAnalyticsController {

    private final SessionAnalyticsService sessionAnalyticsService;

    @GetMapping("/{watchSpaceId}/analytics")
    public Mono<SessionAnalyticsResponse> getAnalytics(
            @PathVariable Long watchSpaceId
    ) {
        return Mono.fromCallable(() -> {
                    SessionAnalyticsService.SessionAnalytics analytics =
                            sessionAnalyticsService.calculateAnalytics(
                                    watchSpaceId
                            );

                    return new SessionAnalyticsResponse(
                            analytics.durationSeconds(),
                            analytics.peakConcurrentParticipants(),
                            analytics.aiQuestions(),
                            analytics.triviaCards(),
                            analytics.chatMessages()
                    );
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
