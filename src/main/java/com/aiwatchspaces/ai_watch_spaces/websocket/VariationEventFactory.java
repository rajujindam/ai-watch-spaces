package com.aiwatchspaces.ai_watch_spaces.websocket;

import com.aiwatchspaces.ai_watch_spaces.dto.VariationOptionResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class VariationEventFactory {

    public WebSocketEvent createVoteOpenEvent(
            Long variationId,
            List<VariationOptionResponse> options,
            Instant closesAt
    ) {
        Map<String, Object> data = Map.of(
                "variationId", variationId,
                "options", options,
                "closesAt", closesAt
        );

        return new WebSocketEvent(
                "room.variation.voteOpen",
                data
        );
    }
    public WebSocketEvent createVoteAppliedEvent(
            Long variationId,
            Long winningOptionId,
            Integer timestampSeconds,
            String assetRef
    ) {
        Map<String, Object> data = Map.of(
                "variationId", variationId,
                "winningOptionId", winningOptionId,
                "timestampSeconds", timestampSeconds,
                "assetRef", assetRef
        );

        return new WebSocketEvent(
                "room.variation.applied",
                data
        );
    }
}