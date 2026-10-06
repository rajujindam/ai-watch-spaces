package com.aiwatchspaces.ai_watch_spaces.websocket;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AiTriviaEventFactory {

    public WebSocketEvent createTriviaEvent(String trivia) {

        Map<String, Object> data = Map.of(
                "trivia", trivia
        );

        return new WebSocketEvent(
                "ai.trivia",
                data
        );
    }
}