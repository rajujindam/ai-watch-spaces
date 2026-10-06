package com.aiwatchspaces.ai_watch_spaces.controller;

import com.aiwatchspaces.ai_watch_spaces.dto.CastVariationVoteRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.OpenVariationVoteRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.VariationVoteResponse;
import com.aiwatchspaces.ai_watch_spaces.dto.VariationVoteResult;
import com.aiwatchspaces.ai_watch_spaces.service.VariationVotingService;
import com.aiwatchspaces.ai_watch_spaces.websocket.VariationEventFactory;
import com.aiwatchspaces.ai_watch_spaces.websocket.WatchSpaceRoomManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/watch-spaces/{watchSpaceId}/variations")
@RequiredArgsConstructor
public class VariationVotingController {

    private final VariationVotingService variationVotingService;
    private final WatchSpaceRoomManager roomManager;
    private final VariationEventFactory variationEventFactory;

    @PostMapping("/{variationId}/vote")
    public Mono<ResponseEntity<VariationVoteResponse>> openVote(
            @PathVariable Long watchSpaceId,
            @PathVariable Long variationId,
            @Valid @RequestBody OpenVariationVoteRequest request,
            JwtAuthenticationToken authentication
    ) {
        Long userId =
                Long.parseLong(Objects.requireNonNull(authentication.getToken().getSubject()));

        return Mono.fromCallable(() ->
                        variationVotingService.openVote(
                                watchSpaceId,
                                userId,
                                variationId,
                                request.durationSeconds()
                        )
                )
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(response -> {

                    var event =
                            variationEventFactory.createVoteOpenEvent(
                                    response.variationId(),
                                    response.options(),
                                    response.closesAt()
                            );

                    return roomManager
                            .broadcast(watchSpaceId, event)
                            .thenReturn(
                                    ResponseEntity
                                            .status(HttpStatus.CREATED)
                                            .body(response)
                            );
                });
    }
    @PostMapping("/{variationId}/votes")
    public Mono<ResponseEntity<Void>> castVote(
            @PathVariable Long watchSpaceId,
            @PathVariable Long variationId,
            @Valid @RequestBody CastVariationVoteRequest request,
            JwtAuthenticationToken authentication
    ) {
        Long userId =
                Long.parseLong(authentication.getToken().getSubject());

        return Mono.fromRunnable(() ->
                        variationVotingService.castVote(
                                watchSpaceId,
                                userId,
                                variationId,
                                request.optionId()
                        )
                )
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(
                        ResponseEntity.ok().build()
                );
    }
    @PostMapping("/vote/close")
    public Mono<ResponseEntity<Void>> closeVote(
            @PathVariable Long watchSpaceId,
            JwtAuthenticationToken authentication
    ) {
        Long userId =
                Long.parseLong(authentication.getToken().getSubject());

        return Mono.fromCallable(() -> {

                    // Host authorization

                    VariationVoteResult result =
                            variationVotingService.closeVote(
                                    watchSpaceId,
                                    userId
                            );

                    return result;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> {

                    var event =
                            variationEventFactory.createVoteAppliedEvent(
                                    result.variationId(),
                                    result.winningOptionId(),
                                    result.timestampSeconds(),
                                    result.assetRef()
                            );

                    return roomManager
                            .broadcast(watchSpaceId, event)
                            .thenReturn(
                                    ResponseEntity.ok().<Void>build()
                            );
                });
    }
}