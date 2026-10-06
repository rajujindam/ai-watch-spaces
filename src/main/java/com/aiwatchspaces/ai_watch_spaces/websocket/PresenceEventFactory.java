package com.aiwatchspaces.ai_watch_spaces.websocket;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PresenceEventFactory {

    public WebSocketEvent createJoinedEvent(Long userId) {

        Map<String, Object> data = Map.of(
                "action", "JOINED",
                "userId", userId
        );

        return new WebSocketEvent(
                "presence.update",
                data
        );
    }

    public WebSocketEvent createLeftEvent(Long userId) {

        Map<String, Object> data = Map.of(
                "action", "LEFT",
                "userId", userId
        );

        return new WebSocketEvent(
                "presence.update",
                data
        );
    }
}