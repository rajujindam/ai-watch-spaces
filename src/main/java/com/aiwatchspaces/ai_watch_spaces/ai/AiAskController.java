package com.aiwatchspaces.ai_watch_spaces.ai;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/watch-spaces")
@RequiredArgsConstructor
public class AiAskController {

    private final AiAskService aiAskService;

    @PostMapping("/{watchSpaceId}/ai/ask")
    public AiAskResponse askQuestion(
            @PathVariable Long watchSpaceId,
            @Valid @RequestBody AiAskRequest request,
            JwtAuthenticationToken authentication
    ) {

        Long userId =
                Long.valueOf(Objects.requireNonNull(authentication.getToken().getSubject()));

        return aiAskService.askQuestion(
                watchSpaceId,
                userId,
                request.question()
        );
    }
}