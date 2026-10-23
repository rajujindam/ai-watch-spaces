package com.aiwatchspaces.ai_watch_spaces.ai;

import com.aiwatchspaces.ai_watch_spaces.dto.WatchSpaceResponse;
import com.aiwatchspaces.ai_watch_spaces.playback.PlaybackState;
import com.aiwatchspaces.ai_watch_spaces.playback.PlaybackStateService;
import com.aiwatchspaces.ai_watch_spaces.service.WatchSpaceEventService;
import com.aiwatchspaces.ai_watch_spaces.service.WatchSpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiAskService {

    private final WatchSpaceService watchSpaceService;
    private final PlaybackStateService playbackStateService;
    private final AiContentEngine aiContentEngine;
    private final WatchSpaceEventService watchSpaceEventService;

    public AiAskResponse askQuestion(
            Long watchSpaceId,
            Long userId,
            String question
    ) {

        WatchSpaceResponse watchSpace =
                watchSpaceService.getWatchSpaceById(
                        watchSpaceId,
                        userId
                );

        PlaybackState playbackState =
                playbackStateService.getState(
                        watchSpaceId,
                        userId
                );
        watchSpaceEventService.recordEvent(
                watchSpaceId,
                userId,
                "AI_QUESTION",
                Map.of()
        );

        double currentPosition =
                playbackState.getPositionSeconds();

        int startSeconds =
                Math.max(
                        0,
                        (int) currentPosition - 60
                );

        int endSeconds =
                (int) currentPosition + 60;


        GroundedAnswer result =
                aiContentEngine.answerQuestionWithSources(
                        question,
                        watchSpace.getTitleId(),
                        startSeconds,
                        endSeconds
                );

        java.util.List<AiAskResponse.SourceEvent> sources =
                result.sources().stream()
                        .map(event -> new AiAskResponse.SourceEvent(
                                event.getId(),
                                event.getTimestampSeconds(),
                                event.getEventType()
                        ))
                        .toList();

        return new AiAskResponse(
                result.answer(),
                sources
        );

    }
}