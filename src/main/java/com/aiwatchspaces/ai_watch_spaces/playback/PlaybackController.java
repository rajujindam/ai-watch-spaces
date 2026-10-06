package com.aiwatchspaces.ai_watch_spaces.playback;

import com.aiwatchspaces.ai_watch_spaces.websocket.PlaybackEventFactory;
import com.aiwatchspaces.ai_watch_spaces.websocket.WatchSpaceRoomManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/watch-spaces/{watchSpaceId}/playback")
@RequiredArgsConstructor
public class PlaybackController {

    private final PlaybackStateService playbackStateService;
    private final WatchSpaceRoomManager roomManager;
    private final PlaybackEventFactory playbackEventFactory;

    @GetMapping
    public ResponseEntity<PlaybackState> getState(
            @PathVariable Long watchSpaceId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long userId = Long.parseLong(jwt.getSubject());

        return ResponseEntity.ok(
                playbackStateService.getState(
                        watchSpaceId,
                        userId
                )
        );
    }

    @PostMapping("/play")
    public Mono<ResponseEntity<PlaybackState>> play(
            @PathVariable Long watchSpaceId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long userId = Long.parseLong(jwt.getSubject());

        return Mono.fromCallable(() -> playbackStateService.play(watchSpaceId, userId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(state -> {
                    var event = playbackEventFactory.createPlaybackUpdate(state);
                    return roomManager.broadcast(watchSpaceId, event)
                            .thenReturn(ResponseEntity.ok(state));
                });
    }

    @PostMapping("/pause")
    public Mono<ResponseEntity<PlaybackState>> pause(
            @PathVariable Long watchSpaceId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long userId = Long.parseLong(jwt.getSubject());

        return Mono.fromCallable(() ->
                        playbackStateService.pause(
                                watchSpaceId,
                                userId
                        )
                )
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(state -> {

                    var event =
                            playbackEventFactory
                                    .createPlaybackUpdate(state);

                    return roomManager
                            .broadcast(
                                    watchSpaceId,
                                    event
                            )
                            .thenReturn(
                                    ResponseEntity.ok(state)
                            );
                });
    }

    @PostMapping("/seek")
    public Mono<ResponseEntity<PlaybackState>> seek(
            @PathVariable Long watchSpaceId,
            @RequestParam double positionSeconds,
            @AuthenticationPrincipal Jwt jwt
    ) {

        Long userId = Long.parseLong(jwt.getSubject());

        return Mono.fromCallable(() ->
                        playbackStateService.seek(
                                watchSpaceId,
                                userId,
                                positionSeconds
                        )
                )
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(state -> {

                    var event =
                            playbackEventFactory
                                    .createPlaybackUpdate(state);

                    return roomManager
                            .broadcast(
                                    watchSpaceId,
                                    event
                            )
                            .thenReturn(
                                    ResponseEntity.ok(state)
                            );
                });
    }
}