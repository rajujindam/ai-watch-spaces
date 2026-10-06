package com.aiwatchspaces.ai_watch_spaces.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TitleResponse {

    private Long id;
    private String name;
    private String description;
    private Integer durationSeconds;
}