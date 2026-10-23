
package com.aiwatchspaces.ai_watch_spaces.websocket;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PlaybackDriftEventFactory {


    public WebSocketEvent createDriftReport(
            double reportedPositionSeconds,
            double expectedPositionSeconds,
            double driftSeconds
    ) {
        double absoluteDrift = Math.abs(driftSeconds);

        String correction;

        if (absoluteDrift <= 0.1) {
            correction = "NONE";
        } else if (absoluteDrift <= 0.25) {
            correction = "ADJUST";
        } else {
            correction = "SEEK";
        }

        return new WebSocketEvent(
                "playback.drift",
                Map.of(
                        "reportedPositionSeconds", reportedPositionSeconds,
                        "expectedPositionSeconds", expectedPositionSeconds,
                        "driftSeconds", driftSeconds,
                        "correction", correction,
                        "targetPositionSeconds", expectedPositionSeconds
                )
        );
    }

}
