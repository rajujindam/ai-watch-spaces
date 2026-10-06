
package com.aiwatchspaces.ai_watch_spaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CreateTimelineEventRequest {

    @NotNull(message = "Timestamp is required")
    @PositiveOrZero(message = "Timestamp cannot be negative")
    private Integer timestampSeconds;

    @NotBlank(message = "Event type is required")
    @Size(max = 50, message = "Event type must not exceed 50 characters")
    private String eventType;

    @NotNull(message = "Payload is required")
    @NotEmpty(message = "Payload cannot be empty")
    private Map<String, Object> payload;
}