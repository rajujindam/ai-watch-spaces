package com.aiwatchspaces.ai_watch_spaces.websocket;

import com.aiwatchspaces.ai_watch_spaces.ai.AiTriviaService;
import com.aiwatchspaces.ai_watch_spaces.service.WatchSpaceEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Objects;

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

                    roomManager.addSession(watchSpaceId, connection);

                    return Mono.usingWhen(
                            Mono.just(connection),

                            // 1. Main WebSocket processing
                            ignored -> recordEventAsync(
                                    watchSpaceId,
                                    userId,
                                    "PARTICIPANT_JOINED"
                            )
                                    .then(
                                            roomManager.broadcast(
                                                    watchSpaceId,
                                                    presenceEventFactory
                                                            .createJoinedEvent(userId)
                                            )
                                    )
                                    .then(
                                            session.receive()
                                                    .concatMap(message ->
                                                            handleMessage(
                                                                    message.getPayloadAsText(),
                                                                    watchSpaceId,
                                                                    userId
                                                            )
                                                    )
                                                    .then()
                                    ),

                            // 2. Cleanup after normal completion
                            resource -> cleanupConnection(
                                    watchSpaceId,
                                    userId,
                                    resource
                            ),

                            // 3. Cleanup after an error
                            (resource, error) -> cleanupConnection(
                                    watchSpaceId,
                                    userId,
                                    resource
                            ),

                            // 4. Cleanup after cancellation
                            resource -> cleanupConnection(
                                    watchSpaceId,
                                    userId,
                                    resource
                            )
                    );
                });
    }

    private Mono<Void> handleMessage(
            String payload,
            Long watchSpaceId,
            Long userId
    ) {
        try {
            WebSocketEvent event = objectMapper.readValue(
                    payload,
                    WebSocketEvent.class
            );

            // AI TRIVIA
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
                                roomManager.broadcast(
                                        watchSpaceId,
                                        triviaEvent
                                )
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
                        chatEventFactory.createChatMessage(
                                userId,
                                chatMessage
                        );

                return recordEventAsync(
                        watchSpaceId,
                        userId,
                        "CHAT_MESSAGE"
                )
                        .then(
                                roomManager.broadcast(
                                        watchSpaceId,
                                        chatEvent
                                )
                        );
            }

            // Ignore unknown event types
            return Mono.empty();

        } catch (JacksonException exception) {
            System.err.println(
                    "Invalid WebSocket event: " + exception.getMessage()
            );

            return Mono.empty();
        }
    }

    private Mono<Void> cleanupConnection(
            Long watchSpaceId,
            Long userId,
            WatchSpaceConnection connection
    ) {
        roomManager.removeSession(watchSpaceId, connection);

        return recordEventAsync(
                watchSpaceId,
                userId,
                "PARTICIPANT_LEFT"
        )
                .then(
                        roomManager.broadcast(
                                watchSpaceId,
                                presenceEventFactory.createLeftEvent(userId)
                        )
                );
    }

    private Mono<Void> recordEventAsync(
            Long watchSpaceId,
            Long userId,
            String eventType
    ) {
        return Mono.<Void>fromRunnable(() ->
                        watchSpaceEventService.recordEvent(
                                watchSpaceId,
                                userId,
                                eventType,
                                Map.of()
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