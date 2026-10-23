
package com.aiwatchspaces.ai_watch_spaces.websocket;

import com.aiwatchspaces.ai_watch_spaces.ai.AiTriviaService;
import com.aiwatchspaces.ai_watch_spaces.dto.TimelineEventResponse;
import com.aiwatchspaces.ai_watch_spaces.playback.PlaybackStateService;
import com.aiwatchspaces.ai_watch_spaces.service.TimelineEventService;
import com.aiwatchspaces.ai_watch_spaces.service.WatchSpaceEventService;
import com.aiwatchspaces.ai_watch_spaces.service.WatchSpaceService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
public class WatchSpaceWebSocketHandler implements WebSocketHandler {

    private final WatchSpaceRoomManager roomManager;
    private final PresenceEventFactory presenceEventFactory;
    private final ObjectMapper objectMapper;
    private final ChatEventFactory chatEventFactory;
    private final AiTriviaService aiTriviaService;
    private final AiTriviaEventFactory aiTriviaEventFactory;
    private final WatchSpaceEventService watchSpaceEventService;
    private final PlaybackStateService playbackStateService;
    private final WatchSpaceService watchSpaceService;
    private final PlaybackEventFactory playbackEventFactory;
    private final PlaybackDriftEventFactory playbackDriftEventFactory;
    private final TimelineEventService timelineEventService;

    // Latest accepted client sequence number per room/user pair.
    private final ConcurrentMap<PlaybackReportKey, Long> lastPlaybackSequence =
            new ConcurrentHashMap<>();

    private record PlaybackReportKey(Long watchSpaceId, Long userId) { }

    // Last authoritative position checked for each watch space.
    private final ConcurrentMap<Long, Integer> lastTriviaCheckPosition =
            new ConcurrentHashMap<>();

    // Prevent duplicate room-wide trivia events within this application instance.
    private final Set<String> triggeredTriviaMarkers =
            ConcurrentHashMap.newKeySet();

    // Markers currently being broadcast.
    private final Set<String> inFlightTriviaMarkers =
            ConcurrentHashMap.newKeySet();

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        Long watchSpaceId = extractWatchSpaceId(session);

        return session.getHandshakeInfo()
                .getPrincipal()
                .flatMap(principal -> {

                    JwtAuthenticationToken authentication =
                            (JwtAuthenticationToken) principal;

                    Long userId = Long.parseLong(
                            Objects.requireNonNull(
                                    authentication.getToken().getSubject()
                            )
                    );

                    WatchSpaceConnection connection =
                            new WatchSpaceConnection(userId, session);

                    return Mono.fromRunnable(() ->
                                    watchSpaceService.getWatchSpaceById(
                                            watchSpaceId,
                                            userId
                                    )
                            )
                            .subscribeOn(Schedulers.boundedElastic())
                            .then(Mono.defer(() -> {
                                roomManager.addSession(watchSpaceId, connection);

                                return Mono.usingWhen(
                                        Mono.just(connection),

                                        ignored -> recordEventAsync(
                                                watchSpaceId,
                                                userId,
                                                "PARTICIPANT_JOINED"
                                        )
                                                .then(roomManager.broadcast(
                                                        watchSpaceId,
                                                        presenceEventFactory
                                                                .createJoinedEvent(userId)
                                                ))
                                                .then(
                                                        Mono.fromCallable(() ->
                                                                        playbackStateService.getState(
                                                                                watchSpaceId,
                                                                                userId
                                                                        )
                                                                )
                                                                .subscribeOn(Schedulers.boundedElastic())
                                                                .flatMap(state ->
                                                                        roomManager.sendToConnection(
                                                                                connection,
                                                                                playbackEventFactory
                                                                                        .createPlaybackUpdate(state)
                                                                        )
                                                                )
                                                )
                                                .then(
                                                        session.receive()
                                                                .concatMap(message ->
                                                                        handleMessage(
                                                                                message.getPayloadAsText(),
                                                                                watchSpaceId,
                                                                                userId,
                                                                                connection
                                                                        )
                                                                )
                                                                .then()
                                                ),

                                        resource -> cleanupConnection(
                                                watchSpaceId,
                                                userId,
                                                resource
                                        ),

                                        (resource, error) -> cleanupConnection(
                                                watchSpaceId,
                                                userId,
                                                resource
                                        ),

                                        resource -> cleanupConnection(
                                                watchSpaceId,
                                                userId,
                                                resource
                                        )
                                );
                            }));
                });
    }

    private Mono<Void> handleMessage(
            String payload,
            Long watchSpaceId,
            Long userId,
            WatchSpaceConnection connection
    ) {
        try {
            WebSocketEvent event = objectMapper.readValue(
                    payload,
                    WebSocketEvent.class
            );

            // PLAYBACK PLAY
            if ("playback.play".equals(event.getType())) {
                return Mono.fromCallable(() ->
                                playbackStateService.play(watchSpaceId, userId)
                        )
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(playbackEventFactory::createPlaybackUpdate)
                        .flatMap(playbackEvent ->
                                roomManager.broadcast(watchSpaceId, playbackEvent)
                        );
            }

            // PLAYBACK PAUSE
            if ("playback.pause".equals(event.getType())) {
                return Mono.fromCallable(() ->
                                playbackStateService.pause(watchSpaceId, userId)
                        )
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(playbackEventFactory::createPlaybackUpdate)
                        .flatMap(playbackEvent ->
                                roomManager.broadcast(watchSpaceId, playbackEvent)
                        );
            }

            // PLAYBACK SEEK
            if ("playback.seek".equals(event.getType())) {
                if (event.getData() == null) {
                    return Mono.empty();
                }

                Object rawPosition = event.getData().get("positionSeconds");

                if (!(rawPosition instanceof Number number)) {
                    return Mono.empty();
                }

                double positionSeconds = number.doubleValue();

                return Mono.fromCallable(() ->
                                playbackStateService.seek(
                                        watchSpaceId,
                                        userId,
                                        positionSeconds
                                )
                        )
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(playbackEventFactory::createPlaybackUpdate)
                        .flatMap(playbackEvent ->
                                roomManager.broadcast(watchSpaceId, playbackEvent)
                        );
            }

            // PLAYBACK POSITION / DRIFT / TIMELINE TRIVIA
            if ("playback.position".equals(event.getType())) {
                if (event.getData() == null) {
                    return Mono.empty();
                }

                Object rawPosition = event.getData().get("positionSeconds");

                if (!(rawPosition instanceof Number number)) {
                    return Mono.empty();
                }

                double reportedPosition = number.doubleValue();

                // Sequence numbers are checked before asynchronous validation so that
                // an older report cannot win a race between concurrent connections.
                // The field is optional for backward compatibility with older clients.
                Object rawSequence = event.getData().get("sequenceNumber");
                if (!acceptPlaybackSequence(watchSpaceId, userId, rawSequence)) {
                    return Mono.empty();
                }

                return Mono.fromCallable(() ->
                                playbackStateService.isValidReportedPosition(
                                        watchSpaceId,
                                        reportedPosition
                                )
                        )
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(valid -> {
                            if (!valid) {
                                return Mono.empty();
                            }

                            return Mono.fromCallable(() ->
                                            playbackStateService.getExpectedPosition(
                                                    watchSpaceId,
                                                    userId
                                            )
                                    )
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .flatMap(expectedPosition -> {
                                        double driftSeconds =
                                                expectedPosition - reportedPosition;

                                        WebSocketEvent driftEvent =
                                                playbackDriftEventFactory.createDriftReport(
                                                        reportedPosition,
                                                        expectedPosition,
                                                        driftSeconds
                                                );

                                        Object correction =
                                                driftEvent.getData().get("correction");

                                        Mono<Void> correctionMono;

                                        if ("NONE".equals(correction)) {
                                            correctionMono = Mono.empty();
                                        } else {
                                            correctionMono =
                                                    roomManager.sendToConnection(
                                                            connection,
                                                            driftEvent
                                                    );
                                        }

                                        return triggerTriviaMarkers(
                                                watchSpaceId,
                                                userId,
                                                expectedPosition
                                        ).then(correctionMono);
                                    });
                        });
            }

            // AI TRIVIA (manual request)
            if ("ai.trivia".equals(event.getType())) {
                return Mono.fromCallable(() ->
                                aiTriviaService.generateTrivia(
                                        watchSpaceId,
                                        userId
                                )
                        )
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(aiTriviaEventFactory::createTriviaEvent)
                        .flatMap(triviaEvent ->
                                roomManager.broadcast(watchSpaceId, triviaEvent)
                        );
            }

            // CHAT MESSAGE
            if ("chat.message".equals(event.getType())) {
                if (event.getData() == null) {
                    return Mono.empty();
                }

                Object messageData = event.getData().get("message");

                if (messageData == null) {
                    return Mono.empty();
                }

                String chatMessage = messageData.toString().trim();

                if (chatMessage.isEmpty()) {
                    return Mono.empty();
                }

                WebSocketEvent chatEvent =
                        chatEventFactory.createChatMessage(userId, chatMessage);

                return recordEventAsync(
                        watchSpaceId,
                        userId,
                        "CHAT_MESSAGE",
                        Map.of("message", chatMessage)
                ).then(
                        roomManager.broadcast(watchSpaceId, chatEvent)
                );
            }

            // CHAT TYPING
            if ("chat.typing".equals(event.getType())) {
                if (event.getData() == null) {
                    return Mono.empty();
                }

                Object typingData = event.getData().get("typing");

                if (!(typingData instanceof Boolean typing)) {
                    return Mono.empty();
                }

                WebSocketEvent typingEvent =
                        chatEventFactory.createTypingEvent(userId, typing);

                return roomManager.broadcastExceptUser(
                        watchSpaceId,
                        userId,
                        typingEvent
                );
            }

            // Ignore unknown event types.
            return Mono.empty();

        } catch (JacksonException exception) {
            System.err.println(
                    "Invalid WebSocket event: " + exception.getMessage()
            );

            return Mono.empty();
        }
    }



    private boolean acceptPlaybackSequence(
            Long watchSpaceId,
            Long userId,
            Object rawSequence
    ) {
        // Older clients do not send sequenceNumber; continue accepting their reports.
        if (rawSequence == null) {
            return true;
        }

        if (!(rawSequence instanceof Number number)) {
            return false;
        }

        final long sequence;
        try {
            sequence = new BigDecimal(number.toString()).longValueExact();
        } catch (NumberFormatException | ArithmeticException exception) {
            return false;
        }

        if (sequence < 0) {
            return false;
        }

        PlaybackReportKey key = new PlaybackReportKey(watchSpaceId, userId);
        AtomicBoolean accepted = new AtomicBoolean(false);

        lastPlaybackSequence.compute(key, (ignored, previousSequence) -> {
            if (previousSequence == null || sequence > previousSequence) {
                accepted.set(true);
                return sequence;
            }
            return previousSequence;
        });

        return accepted.get();
    }

    private Mono<Void> triggerTriviaMarkers(
            Long watchSpaceId,
            Long userId,
            double currentPositionSeconds
    ) {
        return Mono.fromCallable(() ->
                        watchSpaceService.getHostId(watchSpaceId)
                )
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(hostId -> {
                    if (!hostId.equals(userId)) {
                        return Mono.empty();
                    }

                    int currentPosition =
                            (int) Math.floor(currentPositionSeconds);

                    Integer previousPosition =
                            lastTriviaCheckPosition.get(watchSpaceId);

                    if (previousPosition != null
                            && currentPosition <= previousPosition) {
                        return Mono.empty();
                    }

                    int startPosition = previousPosition == null
                            ? 0
                            : previousPosition + 1;

                    return Mono.fromCallable(() ->
                                    watchSpaceService.getWatchSpaceById(
                                            watchSpaceId, userId
                                    ).getTitleId()
                            )
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(titleId ->
                                    Mono.fromCallable(() ->
                                                    timelineEventService.getTriviaEventsInRange(
                                                            titleId,
                                                            startPosition,
                                                            currentPosition
                                                    )
                                            )
                                            .subscribeOn(Schedulers.boundedElastic())
                            )
                            .flatMap(markers ->
                                    Flux.fromIterable(markers)
                                            .concatMap(marker ->
                                                    broadcastTriviaMarker(
                                                            watchSpaceId, marker
                                                    )
                                            )
                                            .then()
                            )
                            .doOnSuccess(ignored ->
                                    lastTriviaCheckPosition.put(
                                            watchSpaceId, currentPosition
                                    )
                            );
                });
    }



    private Mono<Void> broadcastTriviaMarker(
            Long watchSpaceId,
            TimelineEventResponse marker
    ) {
        String markerKey = watchSpaceId + ":" + marker.getId();

        if (triggeredTriviaMarkers.contains(markerKey)) {
            return Mono.empty();
        }

        if (!inFlightTriviaMarkers.add(markerKey)) {
            return Mono.empty();
        }

        WebSocketEvent triviaEvent = new WebSocketEvent(
                "ai.trivia",
                Map.of(
                        "markerId", marker.getId(),
                        "timestampSeconds", marker.getTimestampSeconds(),
                        "payload", marker.getPayload()
                )
        );

        return roomManager.broadcast(watchSpaceId, triviaEvent)
                .doOnSuccess(ignored ->
                        triggeredTriviaMarkers.add(markerKey)
                )
                .doFinally(signal ->
                        inFlightTriviaMarkers.remove(markerKey)
                );
    }


    private Mono<Void> cleanupConnection(
            Long watchSpaceId,
            Long userId,
            WatchSpaceConnection connection
    ) {
        roomManager.removeSession(watchSpaceId, connection);

        Mono<Void> presenceCleanup =
                recordEventAsync(
                        watchSpaceId,
                        userId,
                        "PARTICIPANT_LEFT"
                ).then(
                        roomManager.broadcast(
                                watchSpaceId,
                                presenceEventFactory.createLeftEvent(userId)
                        )
                );

        return presenceCleanup.then(
                Mono.fromCallable(() ->
                                watchSpaceService.getHostId(watchSpaceId)
                        )
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(hostId -> {
                            boolean isHost = hostId.equals(userId);

                            boolean hostStillConnected =
                                    roomManager.hasActiveConnection(
                                            watchSpaceId,
                                            userId
                                    );

                            if (!isHost || hostStillConnected) {
                                return Mono.empty();
                            }

                            return Mono.fromCallable(() ->
                                            playbackStateService.pauseOnHostDisconnect(
                                                    watchSpaceId
                                            )
                                    )
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .flatMap(state ->
                                            roomManager.broadcast(
                                                    watchSpaceId,
                                                    playbackEventFactory
                                                            .createPlaybackUpdate(state)
                                            )
                                    );
                        })
        );
    }

    private Mono<Void> recordEventAsync(
            Long watchSpaceId,
            Long userId,
            String eventType
    ) {
        return recordEventAsync(
                watchSpaceId,
                userId,
                eventType,
                Map.of()
        );
    }

    private Mono<Void> recordEventAsync(
            Long watchSpaceId,
            Long userId,
            String eventType,
            Map<String, Object> metadata
    ) {
        return Mono.<Void>fromRunnable(() ->
                        watchSpaceEventService.recordEvent(
                                watchSpaceId,
                                userId,
                                eventType,
                                metadata
                        )
                )
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Long extractWatchSpaceId(WebSocketSession session) {
        String path = session.getHandshakeInfo()
                .getUri()
                .getPath();

        String[] parts = path.split("/");

        return Long.parseLong(parts[parts.length - 1]);
    }
}
