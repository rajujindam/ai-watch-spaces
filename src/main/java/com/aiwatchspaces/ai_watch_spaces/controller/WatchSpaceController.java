
package com.aiwatchspaces.ai_watch_spaces.controller;

import com.aiwatchspaces.ai_watch_spaces.dto.CreateWatchSpaceRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.ParticipantResponse;
import com.aiwatchspaces.ai_watch_spaces.dto.WatchSpaceResponse;
import com.aiwatchspaces.ai_watch_spaces.service.WatchSpaceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    @PatchMapping("/{watchSpaceId}/lock")
    public ResponseEntity<Void> setWatchSpaceLocked(
            @PathVariable Long watchSpaceId,
            @RequestParam boolean locked,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.parseLong(jwt.getSubject());

        watchSpaceService.setWatchSpaceLocked(
                watchSpaceId,
                userId,
                locked
        );

        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{watchSpaceId}/participants/{participantUserId}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable Long watchSpaceId,
            @PathVariable Long participantUserId,
            @AuthenticationPrincipal Jwt jwt) {

        Long hostId = Long.parseLong(jwt.getSubject());

        watchSpaceService.removeParticipant(
                watchSpaceId,
                hostId,
                participantUserId
        );

        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/{watchSpaceId}/host")
    public ResponseEntity<Void> transferHost(
            @PathVariable Long watchSpaceId,
            @RequestParam Long newHostId,
            @AuthenticationPrincipal Jwt jwt) {

        Long currentHostId = Long.parseLong(jwt.getSubject());

        watchSpaceService.transferHost(
                watchSpaceId,
                currentHostId,
                newHostId
        );

        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/invite")
    public ResponseEntity<Map<String, String>> createInvite(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        String inviteCode = watchSpaceService.createInviteCode(id, userId);

        return ResponseEntity.ok(Map.of(
                "inviteCode", inviteCode,
                "joinEndpoint", "/api/v1/watch-spaces/join/" + inviteCode,
                "joinMethod", "POST",
                "note", "Call joinEndpoint with your access-token Bearer authorization header"
        ));
    }

    @PostMapping("/join/{inviteCode}")
    public ResponseEntity<ParticipantResponse> joinByInvite(
            @PathVariable String inviteCode,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());

        ParticipantResponse response =
                watchSpaceService.joinWatchSpaceByInvite(inviteCode, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}