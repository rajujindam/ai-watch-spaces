
package com.aiwatchspaces.ai_watch_spaces.controller;

import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpaceEvent;
import com.aiwatchspaces.ai_watch_spaces.service.WatchSpaceEventService;
import com.aiwatchspaces.ai_watch_spaces.service.WatchSpaceService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/v1/watch-spaces/{watchSpaceId}/chat")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final WatchSpaceService watchSpaceService;
    private final WatchSpaceEventService watchSpaceEventService;

    @GetMapping("/history")
    public Mono<List<WatchSpaceEvent>> getChatHistory(
            @PathVariable Long watchSpaceId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return Mono.fromCallable(() -> {
                    Long userId;

                    try {
                        userId = Long.valueOf(jwt.getSubject());
                    } catch (NumberFormatException ex) {
                        throw new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid user ID in token"
                        );
                    }

                    watchSpaceService.getWatchSpaceById(
                            watchSpaceId,
                            userId
                    );

                    return watchSpaceEventService
                            .getRecentChatMessages(watchSpaceId);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
