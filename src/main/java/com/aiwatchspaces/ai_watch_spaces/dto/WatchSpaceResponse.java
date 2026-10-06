
package com.aiwatchspaces.ai_watch_spaces.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class WatchSpaceResponse {

    private Long id;
    private String name;
    private Long hostId;
    private Long titleId;
    private LocalDateTime createdAt;

}