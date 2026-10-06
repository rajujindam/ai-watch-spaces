package com.aiwatchspaces.ai_watch_spaces.ai;

import com.aiwatchspaces.ai_watch_spaces.entity.TimelineEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TimelineContextBuilder {

    public String buildContext(
            List<TimelineEvent> events
    ) {

        StringBuilder context =
                new StringBuilder();

        for (TimelineEvent event : events) {

            context.append("At ")
                    .append(event.getTimestampSeconds())
                    .append(" seconds:\n");

            context.append("Event type: ")
                    .append(event.getEventType())
                    .append("\n");

            context.append("Details: ")
                    .append(event.getPayload())
                    .append("\n\n");
        }

        return context.toString();
    }
}