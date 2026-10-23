
package com.aiwatchspaces.ai_watch_spaces.websocket;

import com.aiwatchspaces.ai_watch_spaces.ai.AiTriviaService;
import com.aiwatchspaces.ai_watch_spaces.dto.TimelineEventResponse;
import com.aiwatchspaces.ai_watch_spaces.dto.WatchSpaceResponse;
import com.aiwatchspaces.ai_watch_spaces.playback.PlaybackStateService;
import com.aiwatchspaces.ai_watch_spaces.service.TimelineEventService;
import com.aiwatchspaces.ai_watch_spaces.service.WatchSpaceEventService;
import com.aiwatchspaces.ai_watch_spaces.service.WatchSpaceService;

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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.doubleThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WatchSpaceWebSocketHandlerTest {

    private WatchSpaceRoomManager roomManager;
    private PresenceEventFactory presenceEventFactory;
    private ChatEventFactory chatEventFactory;
    private AiTriviaService aiTriviaService;
    private AiTriviaEventFactory aiTriviaEventFactory;
    private WatchSpaceEventService watchSpaceEventService;
    private PlaybackStateService playbackStateService;
    private WatchSpaceService watchSpaceService;
    private PlaybackEventFactory playbackEventFactory;
    private PlaybackDriftEventFactory playbackDriftEventFactory;
    private WatchSpaceWebSocketHandler handler;
    private TimelineEventService timelineEventService;

    @BeforeEach
    void setUp() {
        roomManager = mock(WatchSpaceRoomManager.class);
        presenceEventFactory = mock(PresenceEventFactory.class);
        chatEventFactory = mock(ChatEventFactory.class);
        aiTriviaService = mock(AiTriviaService.class);
        aiTriviaEventFactory = mock(AiTriviaEventFactory.class);
        watchSpaceEventService = mock(WatchSpaceEventService.class);
        playbackStateService = mock(PlaybackStateService.class);
        watchSpaceService = mock(WatchSpaceService.class);
        playbackEventFactory = mock(PlaybackEventFactory.class);
        playbackDriftEventFactory = mock(PlaybackDriftEventFactory.class);
        timelineEventService = mock(TimelineEventService.class);

        when(roomManager.broadcast(anyLong(), any(WebSocketEvent.class)))
                .thenReturn(Mono.empty());

        when(roomManager.sendToConnection(
                any(WatchSpaceConnection.class),
                any(WebSocketEvent.class)
        )).thenReturn(Mono.empty());

        doReturn(null).when(watchSpaceEventService).recordEvent(
                anyLong(), anyLong(), anyString(), anyMap()
        );

        // Use a different host ID so this test user's disconnect
        // does not trigger the host-disconnect playback logic.
        when(watchSpaceService.getHostId(anyLong())).thenReturn(2L);

        handler = new WatchSpaceWebSocketHandler(
                roomManager,
                presenceEventFactory,
                new ObjectMapper(),
                chatEventFactory,
                aiTriviaService,
                aiTriviaEventFactory,
                watchSpaceEventService,
                playbackStateService,
                watchSpaceService,
                playbackEventFactory,
                playbackDriftEventFactory,
                timelineEventService
        );
    }

    @Test
    void shouldHandleAiTriviaEvent() {
        Long watchSpaceId = 4L;
        Long userId = 1L;

        WebSocketMessage message = mock(WebSocketMessage.class);
        when(message.getPayloadAsText()).thenReturn("""
                {
                    "type": "ai.trivia",
                    "data": {}
                }
                """);

        WebSocketSession session = mock(WebSocketSession.class);
        HandshakeInfo handshakeInfo = mock(HandshakeInfo.class);

        when(session.receive()).thenReturn(Flux.just(message));
        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getUri())
                .thenReturn(URI.create("/ws/watch-spaces/4"));
        when(handshakeInfo.getPrincipal())
                .thenReturn(Mono.just(createAuthentication(userId)));

        stubPresenceEvents(userId);

        String trivia = """
                Question: Which character appeared at 120 seconds?
                Answer: Example Character
                """;

        when(aiTriviaService.generateTrivia(watchSpaceId, userId))
                .thenReturn(trivia);

        WebSocketEvent triviaEvent = new WebSocketEvent(
                "ai.trivia",
                Map.of("trivia", trivia)
        );

        when(aiTriviaEventFactory.createTriviaEvent(trivia))
                .thenReturn(triviaEvent);

        handler.handle(session).block();

        verify(aiTriviaService).generateTrivia(watchSpaceId, userId);
        verify(aiTriviaEventFactory).createTriviaEvent(trivia);
        verify(roomManager, atLeastOnce())
                .broadcast(eq(watchSpaceId), any(WebSocketEvent.class));
    }

    @Test
    void shouldCalculateAndSendPlaybackDriftCorrection() {
        Long watchSpaceId = 4L;
        Long userId = 1L;

        WebSocketMessage message = mock(WebSocketMessage.class);
        when(message.getPayloadAsText()).thenReturn("""
                {
                    "type": "playback.position",
                    "data": {
                        "positionSeconds": 10.0
                    }
                }
                """);

        WebSocketSession session = mock(WebSocketSession.class);
        HandshakeInfo handshakeInfo = mock(HandshakeInfo.class);

        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getUri())
                .thenReturn(URI.create("/ws/watch-spaces/4"));
        when(handshakeInfo.getPrincipal())
                .thenReturn(Mono.just(createAuthentication(userId)));
        when(session.receive()).thenReturn(Flux.just(message));
        when(session.isOpen()).thenReturn(true);

        stubPresenceEvents(userId);

        when(playbackStateService.isValidReportedPosition(4L, 10.0))
                .thenReturn(true);
        when(playbackStateService.getExpectedPosition(4L, 1L))
                .thenReturn(10.3);

        WebSocketEvent driftEvent = new WebSocketEvent(
                "playback.drift",
                Map.of(
                        "reportedPositionSeconds", 10.0,
                        "expectedPositionSeconds", 10.3,
                        "driftSeconds", 0.3,
                        "correction", "SEEK",
                        "targetPositionSeconds", 10.3
                )
        );

        when(playbackDriftEventFactory.createDriftReport(
                anyDouble(),
                anyDouble(),
                anyDouble()
        )).thenReturn(driftEvent);

        handler.handle(session).block();

        verify(playbackStateService).isValidReportedPosition(4L, 10.0);
        verify(playbackStateService).getExpectedPosition(4L, 1L);

        verify(playbackDriftEventFactory).createDriftReport(
                eq(10.0),
                eq(10.3),
                doubleThat(drift -> Math.abs(drift - 0.3) < 0.000001)
        );

        verify(roomManager).sendToConnection(
                argThat(c ->
                        c.getUserId().equals(userId)
                                && c.getSession() == session),
                eq(driftEvent)
        );        verify(roomManager, never()).broadcast(4L, driftEvent);
    }


    @Test
    void shouldIgnoreOutOfOrderPlaybackPositionSequenceNumbers() {
        Long watchSpaceId = 4L;
        Long userId = 1L;

        WebSocketMessage newerMessage = mock(WebSocketMessage.class);
        when(newerMessage.getPayloadAsText()).thenReturn("""
                {
                    "type": "playback.position",
                    "data": {"positionSeconds": 10.0, "sequenceNumber": 5}
                }
                """);

        WebSocketMessage staleMessage = mock(WebSocketMessage.class);
        when(staleMessage.getPayloadAsText()).thenReturn("""
                {
                    "type": "playback.position",
                    "data": {"positionSeconds": 3.0, "sequenceNumber": 4}
                }
                """);

        WebSocketSession session = mock(WebSocketSession.class);
        HandshakeInfo handshakeInfo = mock(HandshakeInfo.class);
        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getUri()).thenReturn(URI.create("/ws/watch-spaces/4"));
        when(handshakeInfo.getPrincipal()).thenReturn(
                Mono.just(createAuthentication(userId))
        );
        when(session.receive()).thenReturn(Flux.just(newerMessage, staleMessage));
        stubPresenceEvents(userId);

        when(playbackStateService.isValidReportedPosition(watchSpaceId, 10.0))
                .thenReturn(true);
        when(playbackStateService.isValidReportedPosition(watchSpaceId, 3.0))
                .thenReturn(true);
        when(playbackStateService.getExpectedPosition(watchSpaceId, userId))
                .thenReturn(10.1);
        when(playbackDriftEventFactory.createDriftReport(
                anyDouble(), anyDouble(), anyDouble()
        )).thenReturn(new WebSocketEvent(
                "playback.drift", Map.of("correction", "NONE")
        ));

        handler.handle(session).block();

        // Only sequence 5 is processed. Sequence 4 is stale and ignored.
        verify(playbackStateService, times(1))
                .getExpectedPosition(watchSpaceId, userId);
        verify(playbackDriftEventFactory, times(1)).createDriftReport(
                anyDouble(), anyDouble(), anyDouble()
        );
    }

    @Test
    void shouldIgnoreMalformedPlaybackSequenceNumber() {
        Long watchSpaceId = 4L;
        Long userId = 1L;

        WebSocketMessage message = mock(WebSocketMessage.class);
        when(message.getPayloadAsText()).thenReturn("""
                {
                    "type": "playback.position",
                    "data": {"positionSeconds": 10.0, "sequenceNumber": "not-a-number"}
                }
                """);

        WebSocketSession session = mock(WebSocketSession.class);
        HandshakeInfo handshakeInfo = mock(HandshakeInfo.class);
        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getUri()).thenReturn(URI.create("/ws/watch-spaces/4"));
        when(handshakeInfo.getPrincipal()).thenReturn(
                Mono.just(createAuthentication(userId))
        );
        when(session.receive()).thenReturn(Flux.just(message));
        stubPresenceEvents(userId);

        when(playbackStateService.isValidReportedPosition(watchSpaceId, 10.0))
                .thenReturn(true);

        handler.handle(session).block();

        verify(playbackStateService, never())
                .getExpectedPosition(anyLong(), anyLong());
        verify(playbackDriftEventFactory, never()).createDriftReport(
                anyDouble(), anyDouble(), anyDouble()
        );
    }

    private void stubPresenceEvents(Long userId) {
        when(presenceEventFactory.createJoinedEvent(userId))
                .thenReturn(new WebSocketEvent(
                        "presence.update",
                        Map.of("action", "JOINED", "userId", userId)
                ));

        when(presenceEventFactory.createLeftEvent(userId))
                .thenReturn(new WebSocketEvent(
                        "presence.update",
                        Map.of("action", "LEFT", "userId", userId)
                ));
    }

    private JwtAuthenticationToken createAuthentication(Long userId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(userId.toString())
                .build();

        return new JwtAuthenticationToken(jwt);
    }

    @Test
    void shouldNotSendCorrectionWhenDriftIsWithin100Milliseconds() {
        Long watchSpaceId = 4L;
        Long userId = 1L;

        WebSocketMessage message = mock(WebSocketMessage.class);
        when(message.getPayloadAsText()).thenReturn("""
            {
                "type": "playback.position",
                "data": {
                    "positionSeconds": 10.0
                }
            }
            """);

        WebSocketSession session = mock(WebSocketSession.class);
        HandshakeInfo handshakeInfo = mock(HandshakeInfo.class);

        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getUri())
                .thenReturn(URI.create("/ws/watch-spaces/4"));
        when(handshakeInfo.getPrincipal())
                .thenReturn(Mono.just(createAuthentication(userId)));
        when(session.receive()).thenReturn(Flux.just(message));

        stubPresenceEvents(userId);

        when(playbackStateService.isValidReportedPosition(4L, 10.0))
                .thenReturn(true);
        when(playbackStateService.getExpectedPosition(4L, 1L))
                .thenReturn(10.05);

        WebSocketEvent driftEvent = new WebSocketEvent(
                "playback.drift",
                Map.of(
                        "reportedPositionSeconds", 10.0,
                        "expectedPositionSeconds", 10.05,
                        "driftSeconds", 0.05,
                        "correction", "NONE",
                        "targetPositionSeconds", 10.05
                )
        );

        when(playbackDriftEventFactory.createDriftReport(
                anyDouble(), anyDouble(), anyDouble()
        )).thenReturn(driftEvent);

        handler.handle(session).block();

        verify(playbackDriftEventFactory).createDriftReport(
                eq(10.0), eq(10.05), anyDouble()
        );

        verify(roomManager, never()).sendToConnection(
                any(WatchSpaceConnection.class),
                any(WebSocketEvent.class)
        );
    }

    @Test
    void shouldIgnoreInvalidPlaybackPosition() {
        Long watchSpaceId = 4L;
        Long userId = 1L;

        WebSocketMessage message = mock(WebSocketMessage.class);
        when(message.getPayloadAsText()).thenReturn("""
            {
                "type": "playback.position",
                "data": {
                    "positionSeconds": -1.0
                }
            }
            """);

        WebSocketSession session = mock(WebSocketSession.class);
        HandshakeInfo handshakeInfo = mock(HandshakeInfo.class);

        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getUri())
                .thenReturn(URI.create("/ws/watch-spaces/4"));
        when(handshakeInfo.getPrincipal())
                .thenReturn(Mono.just(createAuthentication(userId)));
        when(session.receive()).thenReturn(Flux.just(message));

        stubPresenceEvents(userId);

        when(playbackStateService.isValidReportedPosition(4L, -1.0))
                .thenReturn(false);

        handler.handle(session).block();

        verify(playbackStateService).isValidReportedPosition(4L, -1.0);
        verify(playbackStateService, never())
                .getExpectedPosition(anyLong(), anyLong());
        verify(playbackDriftEventFactory, never())
                .createDriftReport(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void shouldSendPlaybackSnapshotToNewConnectionOnReconnect() {
        Long watchSpaceId = 4L;
        Long userId = 1L;

        WebSocketSession session = mock(WebSocketSession.class);
        HandshakeInfo handshakeInfo = mock(HandshakeInfo.class);

        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getUri())
                .thenReturn(URI.create("/ws/watch-spaces/4"));
        when(handshakeInfo.getPrincipal())
                .thenReturn(Mono.just(createAuthentication(userId)));

        when(session.receive()).thenReturn(Flux.empty());

        stubPresenceEvents(userId);

        com.aiwatchspaces.ai_watch_spaces.playback.PlaybackState state =
                new com.aiwatchspaces.ai_watch_spaces.playback.PlaybackState(
                        watchSpaceId);

        state.setStatus(
                com.aiwatchspaces.ai_watch_spaces.playback.PlaybackStatus.PLAYING);
        state.setPositionSeconds(120.0);

        WebSocketEvent snapshotEvent = new WebSocketEvent(
                "playback.update",
                Map.of(
                        "status", "PLAYING",
                        "positionSeconds", 120.0
                )
        );

        when(playbackStateService.getState(watchSpaceId, userId))
                .thenReturn(state);

        when(playbackEventFactory.createPlaybackUpdate(state))
                .thenReturn(snapshotEvent);

        handler.handle(session).block();

        verify(playbackStateService).getState(watchSpaceId, userId);
        verify(playbackEventFactory).createPlaybackUpdate(state);

        verify(roomManager).sendToConnection(
                argThat(connection ->
                        connection.getUserId().equals(userId)
                                && connection.getSession() == session),
                eq(snapshotEvent)
        );
    }
    @Test
    void shouldPausePlaybackAndBroadcastUpdate() {
        Long watchSpaceId = 4L;
        Long userId = 2L; // Host ID configured in setUp()

        WebSocketMessage message = mock(WebSocketMessage.class);
        when(message.getPayloadAsText()).thenReturn("""
            {
                "type": "playback.pause",
                "data": {}
            }
            """);

        WebSocketSession session = mock(WebSocketSession.class);
        HandshakeInfo handshakeInfo = mock(HandshakeInfo.class);

        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getUri())
                .thenReturn(URI.create("/ws/watch-spaces/4"));
        when(handshakeInfo.getPrincipal())
                .thenReturn(Mono.just(createAuthentication(userId)));
        when(session.receive()).thenReturn(Flux.just(message));

        stubPresenceEvents(userId);

        com.aiwatchspaces.ai_watch_spaces.playback.PlaybackState state =
                new com.aiwatchspaces.ai_watch_spaces.playback.PlaybackState(
                        watchSpaceId);

        state.setStatus(
                com.aiwatchspaces.ai_watch_spaces.playback.PlaybackStatus.PAUSED);
        state.setPositionSeconds(120.0);

        WebSocketEvent playbackUpdate = new WebSocketEvent(
                "playback.update",
                Map.of(
                        "status", "PAUSED",
                        "positionSeconds", 120.0
                )
        );

        when(playbackStateService.pause(watchSpaceId, userId))
                .thenReturn(state);
        when(playbackEventFactory.createPlaybackUpdate(state))
                .thenReturn(playbackUpdate);

        handler.handle(session).block();

        verify(playbackStateService).pause(watchSpaceId, userId);
        verify(playbackEventFactory).createPlaybackUpdate(state);
        verify(roomManager).broadcast(watchSpaceId, playbackUpdate);
    }
    @Test
    void shouldPlayPlaybackAndBroadcastUpdate() {
        Long watchSpaceId = 4L;
        Long userId = 2L; // Host ID configured in setUp()

        WebSocketMessage message = mock(WebSocketMessage.class);
        when(message.getPayloadAsText()).thenReturn("""
            {
                "type": "playback.play",
                "data": {}
            }
            """);

        WebSocketSession session = mock(WebSocketSession.class);
        HandshakeInfo handshakeInfo = mock(HandshakeInfo.class);

        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getUri())
                .thenReturn(URI.create("/ws/watch-spaces/4"));
        when(handshakeInfo.getPrincipal())
                .thenReturn(Mono.just(createAuthentication(userId)));
        when(session.receive()).thenReturn(Flux.just(message));

        stubPresenceEvents(userId);

        com.aiwatchspaces.ai_watch_spaces.playback.PlaybackState state =
                new com.aiwatchspaces.ai_watch_spaces.playback.PlaybackState(
                        watchSpaceId);

        state.setStatus(
                com.aiwatchspaces.ai_watch_spaces.playback.PlaybackStatus.PLAYING);
        state.setPositionSeconds(120.0);

        WebSocketEvent playbackUpdate = new WebSocketEvent(
                "playback.update",
                Map.of(
                        "status", "PLAYING",
                        "positionSeconds", 120.0
                )
        );

        when(playbackStateService.play(watchSpaceId, userId))
                .thenReturn(state);
        when(playbackEventFactory.createPlaybackUpdate(state))
                .thenReturn(playbackUpdate);

        handler.handle(session).block();

        verify(playbackStateService).play(watchSpaceId, userId);
        verify(playbackEventFactory).createPlaybackUpdate(state);
        verify(roomManager).broadcast(watchSpaceId, playbackUpdate);
    }
    @Test
    void shouldSeekPlaybackAndBroadcastUpdate() {
        Long watchSpaceId = 4L;
        Long userId = 2L; // Host ID configured in setUp()

        WebSocketMessage message = mock(WebSocketMessage.class);
        when(message.getPayloadAsText()).thenReturn("""
            {
                "type": "playback.seek",
                "data": {
                    "positionSeconds": 180.0
                }
            }
            """);

        WebSocketSession session = mock(WebSocketSession.class);
        HandshakeInfo handshakeInfo = mock(HandshakeInfo.class);

        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getUri())
                .thenReturn(URI.create("/ws/watch-spaces/4"));
        when(handshakeInfo.getPrincipal())
                .thenReturn(Mono.just(createAuthentication(userId)));
        when(session.receive()).thenReturn(Flux.just(message));

        stubPresenceEvents(userId);

        com.aiwatchspaces.ai_watch_spaces.playback.PlaybackState state =
                new com.aiwatchspaces.ai_watch_spaces.playback.PlaybackState(
                        watchSpaceId);

        state.setStatus(
                com.aiwatchspaces.ai_watch_spaces.playback.PlaybackStatus.PLAYING);
        state.setPositionSeconds(180.0);

        WebSocketEvent playbackUpdate = new WebSocketEvent(
                "playback.update",
                Map.of(
                        "status", "PLAYING",
                        "positionSeconds", 180.0
                )
        );

        when(playbackStateService.seek(watchSpaceId, userId, 180.0))
                .thenReturn(state);
        when(playbackEventFactory.createPlaybackUpdate(state))
                .thenReturn(playbackUpdate);

        handler.handle(session).block();

        verify(playbackStateService).seek(watchSpaceId, userId, 180.0);
        verify(playbackEventFactory).createPlaybackUpdate(state);
        verify(roomManager).broadcast(watchSpaceId, playbackUpdate);
    }
    @Test
    void shouldIgnoreSeekWhenPositionIsMissing() {
        Long watchSpaceId = 4L;
        Long userId = 2L;

        WebSocketMessage message = mock(WebSocketMessage.class);
        when(message.getPayloadAsText()).thenReturn("""
            {
                "type": "playback.seek",
                "data": {}
            }
            """);

        WebSocketSession session = mock(WebSocketSession.class);
        HandshakeInfo handshakeInfo = mock(HandshakeInfo.class);

        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getUri())
                .thenReturn(URI.create("/ws/watch-spaces/4"));
        when(handshakeInfo.getPrincipal())
                .thenReturn(Mono.just(createAuthentication(userId)));
        when(session.receive()).thenReturn(Flux.just(message));

        stubPresenceEvents(userId);

        handler.handle(session).block();

        verify(playbackStateService, never())
                .seek(anyLong(), anyLong(), anyDouble());
        verify(playbackEventFactory, never())
                .createPlaybackUpdate(any());
    }
    @Test
    void shouldNotBroadcastPauseUpdateWhenNonHostAttemptsToPause() {
        Long watchSpaceId = 4L;
        Long userId = 1L; // Non-host; setUp() configures host ID as 2L

        WebSocketMessage message = mock(WebSocketMessage.class);
        when(message.getPayloadAsText()).thenReturn("""
            {
                "type": "playback.pause",
                "data": {}
            }
            """);

        WebSocketSession session = mock(WebSocketSession.class);
        HandshakeInfo handshakeInfo = mock(HandshakeInfo.class);

        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getUri())
                .thenReturn(URI.create("/ws/watch-spaces/4"));
        when(handshakeInfo.getPrincipal())
                .thenReturn(Mono.just(createAuthentication(userId)));
        when(session.receive()).thenReturn(Flux.just(message));

        stubPresenceEvents(userId);

        when(playbackStateService.pause(watchSpaceId, userId))
                .thenThrow(new com.aiwatchspaces.ai_watch_spaces.exception
                        .WatchSpaceAccessDeniedException(
                        "Only the host can control playback"
                ));

        org.junit.jupiter.api.Assertions.assertThrows(
                com.aiwatchspaces.ai_watch_spaces.exception
                        .WatchSpaceAccessDeniedException.class,
                () -> handler.handle(session).block()
        );

        verify(playbackStateService).pause(watchSpaceId, userId);
        verify(roomManager, never()).broadcast(
                eq(watchSpaceId),
                argThat(event -> "playback.update".equals(event.getType()))
        );
    }

    @Test
    void shouldIncludeTimestampZeroOnFirstHostPositionReport() {
        Long roomId = 4L;
        Long hostId = 2L;

        WebSocketMessage message = mock(WebSocketMessage.class);
        when(message.getPayloadAsText()).thenReturn("""
            {
                "type": "playback.position",
                "data": {"positionSeconds": 10.0}
            }
            """);

        WebSocketSession session = mock(WebSocketSession.class);
        HandshakeInfo handshakeInfo = mock(HandshakeInfo.class);

        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getUri()).thenReturn(
                URI.create("/ws/watch-spaces/4")
        );
        when(handshakeInfo.getPrincipal()).thenReturn(
                Mono.just(createAuthentication(hostId))
        );
        when(session.receive()).thenReturn(Flux.just(message));

        stubPresenceEvents(hostId);

        when(playbackStateService.isValidReportedPosition(roomId, 10.0))
                .thenReturn(true);
        when(playbackStateService.getExpectedPosition(roomId, hostId))
                .thenReturn(10.0);
        when(playbackDriftEventFactory.createDriftReport(
                anyDouble(), anyDouble(), anyDouble()
        )).thenReturn(new WebSocketEvent(
                "playback.drift", Map.of("correction", "NONE")
        ));

        when(watchSpaceService.getHostId(roomId)).thenReturn(hostId);

        WatchSpaceResponse room = mock(WatchSpaceResponse.class);
        when(room.getTitleId()).thenReturn(10L);
        when(watchSpaceService.getWatchSpaceById(roomId, hostId))
                .thenReturn(room);

        TimelineEventResponse marker = new TimelineEventResponse(
                100L, 10L, 0, "TRIVIA",
                Map.of("question", "Question", "answer", "Answer")
        );

        when(timelineEventService.getTriviaEventsInRange(10L, 0, 10))
                .thenReturn(List.of(marker));

        handler.handle(session).block();

        verify(timelineEventService)
                .getTriviaEventsInRange(10L, 0, 10);

        verify(roomManager).broadcast(
                eq(roomId),
                argThat(event ->
                        "ai.trivia".equals(event.getType())
                                && Long.valueOf(100L).equals(
                                event.getData().get("markerId")
                        )
                )
        );
    }

    @Test
    void shouldNotBroadcastSameTriviaMarkerTwice() {
        Long roomId = 4L;
        Long hostId = 2L;

        WebSocketMessage first = mock(WebSocketMessage.class);
        when(first.getPayloadAsText()).thenReturn("""
            {"type":"playback.position","data":{"positionSeconds":10.0}}
            """);

        WebSocketMessage second = mock(WebSocketMessage.class);
        when(second.getPayloadAsText()).thenReturn("""
            {"type":"playback.position","data":{"positionSeconds":11.0}}
            """);

        WebSocketSession session = mock(WebSocketSession.class);
        HandshakeInfo handshakeInfo = mock(HandshakeInfo.class);

        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getUri()).thenReturn(
                URI.create("/ws/watch-spaces/4")
        );
        when(handshakeInfo.getPrincipal()).thenReturn(
                Mono.just(createAuthentication(hostId))
        );
        when(session.receive()).thenReturn(Flux.just(first, second));

        stubPresenceEvents(hostId);

        when(playbackStateService.isValidReportedPosition(
                eq(roomId), anyDouble()
        )).thenReturn(true);

        when(playbackStateService.getExpectedPosition(
                eq(roomId), eq(hostId)
        )).thenReturn(10.0, 11.0);

        when(playbackDriftEventFactory.createDriftReport(
                anyDouble(), anyDouble(), anyDouble()
        )).thenReturn(new WebSocketEvent(
                "playback.drift", Map.of("correction", "NONE")
        ));

        when(watchSpaceService.getHostId(roomId)).thenReturn(hostId);

        WatchSpaceResponse room = mock(WatchSpaceResponse.class);
        when(room.getTitleId()).thenReturn(10L);
        when(watchSpaceService.getWatchSpaceById(roomId, hostId))
                .thenReturn(room);

        TimelineEventResponse marker = new TimelineEventResponse(
                101L, 10L, 5, "TRIVIA",
                Map.of("question", "Question", "answer", "Answer")
        );

        when(timelineEventService.getTriviaEventsInRange(10L, 0, 10))
                .thenReturn(List.of(marker));
        when(timelineEventService.getTriviaEventsInRange(10L, 11, 11))
                .thenReturn(List.of());

        handler.handle(session).block();

        verify(roomManager, times(1)).broadcast(
                eq(roomId),
                argThat(event ->
                        "ai.trivia".equals(event.getType())
                                && Long.valueOf(101L).equals(
                                event.getData().get("markerId")
                        )
                )
        );
    }

    @Test
    void shouldRetryMarkerWhenBroadcastFails() {
        Long roomId = 4L;
        Long hostId = 2L;

        WebSocketMessage first = mock(WebSocketMessage.class);
        when(first.getPayloadAsText()).thenReturn("""
            {"type":"playback.position","data":{"positionSeconds":10.0}}
            """);

        WebSocketSession firstSession = mock(WebSocketSession.class);
        HandshakeInfo firstHandshake = mock(HandshakeInfo.class);

        when(firstSession.getHandshakeInfo()).thenReturn(firstHandshake);
        when(firstHandshake.getUri()).thenReturn(
                URI.create("/ws/watch-spaces/4")
        );
        when(firstHandshake.getPrincipal()).thenReturn(
                Mono.just(createAuthentication(hostId))
        );
        when(firstSession.receive()).thenReturn(Flux.just(first));
        stubPresenceEvents(hostId);

        when(playbackStateService.isValidReportedPosition(roomId, 10.0))
                .thenReturn(true);
        when(playbackStateService.getExpectedPosition(roomId, hostId))
                .thenReturn(10.0);
        when(playbackDriftEventFactory.createDriftReport(
                anyDouble(), anyDouble(), anyDouble()
        )).thenReturn(new WebSocketEvent(
                "playback.drift", Map.of("correction", "NONE")
        ));
        when(watchSpaceService.getHostId(roomId)).thenReturn(hostId);

        WatchSpaceResponse room = mock(WatchSpaceResponse.class);
        when(room.getTitleId()).thenReturn(10L);
        when(watchSpaceService.getWatchSpaceById(roomId, hostId))
                .thenReturn(room);

        TimelineEventResponse marker = new TimelineEventResponse(
                102L, 10L, 5, "TRIVIA",
                Map.of("question", "Question", "answer", "Answer")
        );

        when(timelineEventService.getTriviaEventsInRange(10L, 0, 10))
                .thenReturn(List.of(marker));

        when(roomManager.broadcast(
                eq(roomId),
                argThat(event -> "ai.trivia".equals(event.getType()))
        )).thenReturn(
                Mono.error(new RuntimeException("Broadcast failed")),
                Mono.empty()
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> handler.handle(firstSession).block()
        );

        WebSocketMessage retry = mock(WebSocketMessage.class);
        when(retry.getPayloadAsText()).thenReturn("""
            {"type":"playback.position","data":{"positionSeconds":10.0}}
            """);

        WebSocketSession retrySession = mock(WebSocketSession.class);
        HandshakeInfo retryHandshake = mock(HandshakeInfo.class);

        when(retrySession.getHandshakeInfo()).thenReturn(retryHandshake);
        when(retryHandshake.getUri()).thenReturn(
                URI.create("/ws/watch-spaces/4")
        );
        when(retryHandshake.getPrincipal()).thenReturn(
                Mono.just(createAuthentication(hostId))
        );
        when(retrySession.receive()).thenReturn(Flux.just(retry));
        stubPresenceEvents(hostId);

        handler.handle(retrySession).block();

        verify(timelineEventService, times(2))
                .getTriviaEventsInRange(10L, 0, 10);
    }

}
