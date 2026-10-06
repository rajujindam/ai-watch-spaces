
package com.aiwatchspaces.ai_watch_spaces.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ParticipantResponse {

    private Long id;
    private Long watchSpaceId;
    private Long userId;
    private LocalDateTime joinedAt;
}