package com.aiwatchspaces.ai_watch_spaces.websocket;

import com.aiwatchspaces.ai_watch_spaces.ai.AiTriviaService;
import com.aiwatchspaces.ai_watch_spaces.service.WatchSpaceEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WatchSpaceWebSocketHandlerTest {

    private WatchSpaceRoomManager roomManager;
    private PresenceEventFactory presenceEventFactory;
    private ChatEventFactory chatEventFactory;
    private AiTriviaService aiTriviaService;
    private AiTriviaEventFactory aiTriviaEventFactory;
    private WatchSpaceWebSocketHandler handler;
    private WatchSpaceEventService watchSpaceEventService;

    @BeforeEach
    void setUp() {

        roomManager = mock(WatchSpaceRoomManager.class);
        presenceEventFactory = mock(PresenceEventFactory.class);
        chatEventFactory = mock(ChatEventFactory.class);
        aiTriviaService = mock(AiTriviaService.class);
        aiTriviaEventFactory = mock(AiTriviaEventFactory.class);
        watchSpaceEventService = mock(WatchSpaceEventService.class);

        ObjectMapper objectMapper = new ObjectMapper();

        handler = new WatchSpaceWebSocketHandler(
                roomManager,
                presenceEventFactory,
                objectMapper,
                chatEventFactory,
                aiTriviaService,
                aiTriviaEventFactory,
                watchSpaceEventService
        );
    }

    @Test
    void shouldHandleAiTriviaEvent() {

        Long watchSpaceId = 4L;
        Long userId = 1L;

        // Mock WebSocket session
        WebSocketSession session =
                mock(WebSocketSession.class);

        // Mock WebSocket message
        WebSocketMessage message =
                mock(WebSocketMessage.class);

        when(message.getPayloadAsText())
                .thenReturn("""
                        {
                            "type": "ai.trivia",
                            "data": {}
                        }
                        """);

        when(session.receive())
                .thenReturn(Flux.just(message));

        // Mock handshake
        HandshakeInfo handshakeInfo =
                mock(HandshakeInfo.class);

        when(session.getHandshakeInfo())
                .thenReturn(handshakeInfo);

        when(handshakeInfo.getUri())
                .thenReturn(
                        URI.create("/ws/watch-spaces/4")
                );

        // Mock JWT authentication
        JwtAuthenticationToken authentication =
                mock(JwtAuthenticationToken.class);

        Jwt jwt =
                mock(Jwt.class);

        when(jwt.getSubject())
                .thenReturn("1");

        when(authentication.getToken())
                .thenReturn(jwt);

        when(handshakeInfo.getPrincipal())
                .thenReturn(
                        Mono.just(authentication)
                );

        // Mock JOINED presence event
        WebSocketEvent joinedEvent =
                new WebSocketEvent(
                        "presence.update",
                        Map.of(
                                "action", "JOINED",
                                "userId", userId
                        )
                );

        when(presenceEventFactory.createJoinedEvent(userId))
                .thenReturn(joinedEvent);

        // Mock LEFT presence event
        WebSocketEvent leftEvent =
                new WebSocketEvent(
                        "presence.update",
                        Map.of(
                                "action", "LEFT",
                                "userId", userId
                        )
                );

        when(presenceEventFactory.createLeftEvent(userId))
                .thenReturn(leftEvent);

        // Mock trivia service
        String trivia =
                """
                Question: Which character appeared at 120 seconds?
                Answer: Example Character
                """;

        when(aiTriviaService.generateTrivia(
                eq(watchSpaceId),
                eq(userId)
        )).thenReturn(trivia);

        // Mock trivia event
        WebSocketEvent triviaEvent =
                new WebSocketEvent(
                        "ai.trivia",
                        Map.of(
                                "trivia",
                                trivia
                        )
                );

        when(aiTriviaEventFactory.createTriviaEvent(
                eq(trivia)
        )).thenReturn(triviaEvent);

        // Mock all room broadcasts
        when(roomManager.broadcast(
                eq(watchSpaceId),
                any(WebSocketEvent.class)
        )).thenReturn(Mono.empty());

        // Execute handler
        handler.handle(session)
                .block();

        // Verify AI trivia service was called
        verify(aiTriviaService)
                .generateTrivia(
                        watchSpaceId,
                        userId
                );

        // Verify trivia event was created
        verify(aiTriviaEventFactory)
                .createTriviaEvent(trivia);

        // Verify room broadcast happened
        verify(roomManager, atLeastOnce())
                .broadcast(
                        eq(watchSpaceId),
                        any(WebSocketEvent.class)
                );
    }
}