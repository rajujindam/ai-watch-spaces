package com.aiwatchspaces.ai_watch_spaces.websocket;

import com.aiwatchspaces.ai_watch_spaces.playback.PlaybackState;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PlaybackEventFactory {

    public WebSocketEvent createPlaybackUpdate(PlaybackState playbackState) {

        Map<String, Object> data = Map.of(
                "status", playbackState.getStatus(),
                "positionSeconds", playbackState.getPositionSeconds()
        );

        return new WebSocketEvent("playback.update",data);
    }
}