package com.aiwatchspaces.ai_watch_spaces.playback;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
public class PlaybackState {

    private final Long watchSpaceId;

    @Setter
    private PlaybackStatus status;

    @Setter
    private double positionSeconds;

    @Setter
    private Instant lastUpdatedAt;

    @Setter
    private Long updatedBy;

    public PlaybackState(Long watchSpaceId) {
        this.watchSpaceId = watchSpaceId;
        this.status = PlaybackStatus.PAUSED;
        this.positionSeconds = 0;
        this.lastUpdatedAt = Instant.now();
        this.updatedBy = null;
    }
}