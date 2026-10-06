package com.aiwatchspaces.ai_watch_spaces.controller;

import com.aiwatchspaces.ai_watch_spaces.dto.CreateInteractionRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.InteractionResponse;
import com.aiwatchspaces.ai_watch_spaces.service.InteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping
    public Mono<ResponseEntity<InteractionResponse>> recordInteraction(
            @Valid @RequestBody CreateInteractionRequest request,
            JwtAuthenticationToken authentication
    ) {
        Long userId = Long.parseLong(
                authentication.getToken().getSubject()
        );

        return Mono.fromCallable(() ->
                        interactionService.recordInteraction(
                                userId,
                                request
                        ))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response ->
                        ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response)
                );
    }
}