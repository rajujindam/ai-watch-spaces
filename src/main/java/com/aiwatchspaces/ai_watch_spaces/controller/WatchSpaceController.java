
package com.aiwatchspaces.ai_watch_spaces.controller;

import com.aiwatchspaces.ai_watch_spaces.dto.CreateWatchSpaceRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.ParticipantResponse;
import com.aiwatchspaces.ai_watch_spaces.dto.WatchSpaceResponse;
import com.aiwatchspaces.ai_watch_spaces.service.WatchSpaceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/watch-spaces")
@RequiredArgsConstructor
public class WatchSpaceController {

    private final WatchSpaceService watchSpaceService;

    @PostMapping
    public ResponseEntity<WatchSpaceResponse> createWatchSpace(
            @Valid @RequestBody CreateWatchSpaceRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.parseLong(jwt.getSubject());

        WatchSpaceResponse response =
                watchSpaceService.createWatchSpace(request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WatchSpaceResponse> getWatchSpaceById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.parseLong(jwt.getSubject());

        WatchSpaceResponse response =
                watchSpaceService.getWatchSpaceById(id, userId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/participants")
    public ResponseEntity<ParticipantResponse> joinWatchSpace(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.parseLong(jwt.getSubject());

        ParticipantResponse response =
                watchSpaceService.joinWatchSpace(id, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<List<ParticipantResponse>> getParticipants(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.parseLong(jwt.getSubject());

        List<ParticipantResponse> participants =
                watchSpaceService.getParticipants(id, userId);

        return ResponseEntity.ok(participants);
    }
    @PostMapping("/{id}/end")
    public ResponseEntity<WatchSpaceResponse> endWatchSpace(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        WatchSpaceResponse response =
                watchSpaceService.endWatchSpace(id, userId);

        return ResponseEntity.ok(response);
    }
}