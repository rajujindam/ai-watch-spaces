package com.aiwatchspaces.ai_watch_spaces.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    private final WatchSpaceWebSocketHandler webSocketHandler;

    @Bean
    public HandlerMapping webSocketHandlerMapping() {

        Map<String, WebSocketHandler> handlers = new HashMap<>();

        handlers.put(
                "/ws/watch-spaces/**",
                webSocketHandler
        );

        SimpleUrlHandlerMapping mapping =
                new SimpleUrlHandlerMapping();

        mapping.setUrlMap(handlers);
        mapping.setOrder(-1);

        return mapping;
    }
}