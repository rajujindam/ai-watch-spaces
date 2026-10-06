
package com.aiwatchspaces.ai_watch_spaces.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinWatchSpaceRequest {

    @NotNull(message = "User ID is required")
    private Long userId;
}