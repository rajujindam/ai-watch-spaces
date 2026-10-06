package com.aiwatchspaces.ai_watch_spaces.websocket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.socket.WebSocketSession;

@Getter
@RequiredArgsConstructor
public class WatchSpaceConnection {

    private final Long userId;
    private final WebSocketSession session;
}