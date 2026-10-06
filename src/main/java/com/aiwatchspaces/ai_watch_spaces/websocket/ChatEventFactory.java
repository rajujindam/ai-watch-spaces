package com.aiwatchspaces.ai_watch_spaces.websocket;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class ChatEventFactory {

    public WebSocketEvent createChatMessage(
            Long userId,
            String message
    ) {

        Map<String, Object> data = Map.of(
                "userId", userId,
                "message", message,
                "timestamp", Instant.now().toString()
        );

        return new WebSocketEvent(
                "chat.message",
                data
        );
    }
}