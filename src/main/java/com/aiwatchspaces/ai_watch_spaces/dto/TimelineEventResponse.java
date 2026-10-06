
package com.aiwatchspaces.ai_watch_spaces.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class TimelineEventResponse {

    private Long id;
    private Long titleId;
    private Integer timestampSeconds;
    private String eventType;
    private Map<String, Object> payload;
}