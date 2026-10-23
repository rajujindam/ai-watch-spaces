
package com.aiwatchspaces.ai_watch_spaces.ai;

import com.aiwatchspaces.ai_watch_spaces.entity.TimelineEvent;

import java.util.List;

public record GroundedAnswer(
        String answer,
        List<TimelineEvent> sources
) {
}
