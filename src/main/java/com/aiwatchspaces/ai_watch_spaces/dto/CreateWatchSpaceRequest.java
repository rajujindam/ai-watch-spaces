package com.aiwatchspaces.ai_watch_spaces.dto;

import com.aiwatchspaces.ai_watch_spaces.entity.AiVerbosity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWatchSpaceRequest {

    @NotBlank(message = "Watch space name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Title ID is required")
    private Long titleId;

    @NotNull(message = "Maximum participants is required")
    @Min(value = 1, message = "Maximum participants must be at least 1")
    @Max(value = 50, message = "Maximum participants must not exceed 50")
    private Integer maxParticipants;

    @NotNull(message = "AI verbosity is required")
    private AiVerbosity aiVerbosity;

    @NotNull(message = "Voting enabled value is required")
    private Boolean votingEnabled;
}