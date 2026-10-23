
package com.aiwatchspaces.ai_watch_spaces.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WatchSpaceRoomManagerTest {

    private WatchSpaceRoomManager roomManager;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        roomManager = new WatchSpaceRoomManager(objectMapper);
    }

    @Test
    void shouldSendEventOnlyToSpecifiedConnection() {
        Long watchSpaceId = 4L;

        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);

        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);

        WatchSpaceConnection connection1 =
                new WatchSpaceConnection(1L, session1);

        WatchSpaceConnection connection2 =
                new WatchSpaceConnection(2L, session2);

        roomManager.addSession(watchSpaceId, connection1);
        roomManager.addSession(watchSpaceId, connection2);

        WebSocketMessage outgoingMessage =
                mock(WebSocketMessage.class);

        when(session1.textMessage(anyString()))
                .thenReturn(outgoingMessage);

        when(session1.send(any(Mono.class)))
                .thenReturn(Mono.empty());

        WebSocketEvent event = new WebSocketEvent(
                "playback.drift",
                java.util.Map.of(
                        "correction", "SEEK",
                        "targetPositionSeconds", 10.3
                )
        );

        roomManager.sendToConnection(connection1, event).block();

        verify(session1).send(any(Mono.class));
        verify(session2, never()).send(any(Mono.class));
    }

    @Test
    void shouldReturnOnlyConnectionsRegisteredInRoom() {
        Long watchSpaceId = 4L;

        WebSocketSession session = mock(WebSocketSession.class);

        WatchSpaceConnection connection =
                new WatchSpaceConnection(1L, session);

        roomManager.addSession(watchSpaceId, connection);

        Set<WatchSpaceConnection> connections =
                roomManager.getConnections(watchSpaceId);

        assertEquals(1, connections.size());
        assertTrue(connections.contains(connection));
    }

    @Test
    void shouldRemoveConnectionFromRoom() {
        Long watchSpaceId = 4L;

        WebSocketSession session = mock(WebSocketSession.class);

        WatchSpaceConnection connection =
                new WatchSpaceConnection(1L, session);

        roomManager.addSession(watchSpaceId, connection);
        roomManager.removeSession(watchSpaceId, connection);

        assertTrue(roomManager.getConnections(watchSpaceId).isEmpty());
    }
}
