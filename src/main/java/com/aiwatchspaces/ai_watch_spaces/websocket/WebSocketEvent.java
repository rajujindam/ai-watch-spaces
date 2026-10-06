package com.aiwatchspaces.ai_watch_spaces.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketEvent {

    private String type;
    private Map<String, Object> data;
}