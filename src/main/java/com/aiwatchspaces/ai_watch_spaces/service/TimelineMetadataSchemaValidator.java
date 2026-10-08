package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.dto.CreateTimelineEventRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.CreateTimelineMetadataRequest;
import com.aiwatchspaces.ai_watch_spaces.exception.InvalidTimelineRequestException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TimelineMetadataSchemaValidator {

    private static final Set<String> ALLOWED_EVENT_TYPES = Set.of(
            "trivia",
            "character",
            "variation_point",
            "glossary"
    );

    public void validate(CreateTimelineMetadataRequest request) {

        if (request.getEvents() == null || request.getEvents().isEmpty()) {
            throw new InvalidTimelineRequestException(
                    "Timeline must contain at least one event"
            );
        }

        for (CreateTimelineEventRequest event : request.getEvents()) {

            if (!ALLOWED_EVENT_TYPES.contains(event.getEventType())) {
                throw new InvalidTimelineRequestException(
                        "Unsupported event type: " + event.getEventType()
                                + ". Allowed types: "
                                + ALLOWED_EVENT_TYPES
                );
            }

            if (event.getPayload() == null
                    || event.getPayload().isEmpty()) {
                throw new InvalidTimelineRequestException(
                        "Payload cannot be empty for event type: "
                                + event.getEventType()
                );
            }
        }
    }
}