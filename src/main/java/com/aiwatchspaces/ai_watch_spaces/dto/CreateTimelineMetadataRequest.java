package com.aiwatchspaces.ai_watch_spaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateTimelineMetadataRequest {

    @NotNull(message = "Title ID is required")
    private Long titleId;

    @NotEmpty(message = "Timeline must contain at least one event")
    @Valid
    private List<CreateTimelineEventRequest> events;
}