package com.aiwatchspaces.ai_watch_spaces.ai;

import com.aiwatchspaces.ai_watch_spaces.dto.WatchSpaceResponse;
import com.aiwatchspaces.ai_watch_spaces.playback.PlaybackState;
import com.aiwatchspaces.ai_watch_spaces.playback.PlaybackStateService;
import com.aiwatchspaces.ai_watch_spaces.service.WatchSpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTriviaService {

    private final WatchSpaceService watchSpaceService;
    private final PlaybackStateService playbackStateService;
    private final AiContentEngine aiContentEngine;

    public String generateTrivia(
            Long watchSpaceId,
            Long userId
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

        return aiContentEngine.generateTrivia(
                watchSpace.getTitleId(),
                startSeconds,
                endSeconds
        );
    }
}