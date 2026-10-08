package com.aiwatchspaces.ai_watch_spaces.dto;

import com.aiwatchspaces.ai_watch_spaces.entity.AiVerbosity;
import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpaceStatus;
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
    private WatchSpaceStatus status;
    private Integer maxParticipants;
    private AiVerbosity aiVerbosity;
    private Boolean votingEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime endedAt;
}