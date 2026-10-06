package com.aiwatchspaces.ai_watch_spaces.ai;

import com.aiwatchspaces.ai_watch_spaces.dto.WatchSpaceResponse;
import com.aiwatchspaces.ai_watch_spaces.playback.PlaybackState;
import com.aiwatchspaces.ai_watch_spaces.playback.PlaybackStateService;
import com.aiwatchspaces.ai_watch_spaces.service.WatchSpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiAskService {

    private final WatchSpaceService watchSpaceService;
    private final PlaybackStateService playbackStateService;
    private final AiContentEngine aiContentEngine;

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

        double currentPosition =
                playbackState.getPositionSeconds();

        int startSeconds =
                Math.max(
                        0,
                        (int) currentPosition - 60
                );

        int endSeconds =
                (int) currentPosition + 60;

        String answer =
                aiContentEngine.answerQuestion(
                        question,
                        watchSpace.getTitleId(),
                        startSeconds,
                        endSeconds
                );

        return new AiAskResponse(answer);
    }
}