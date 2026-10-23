
package com.aiwatchspaces.ai_watch_spaces.ai;

import java.util.List;

public record AiAskResponse(
        String answer,
        List<SourceEvent> sources
) {
    public record SourceEvent(
            Long eventId,
            Integer timestampSeconds,
            String eventType
    ) {
    }
}
