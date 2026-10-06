package com.aiwatchspaces.ai_watch_spaces.websocket;

import com.aiwatchspaces.ai_watch_spaces.ai.AiTriviaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import reactor.core.scheduler.Schedulers;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class WatchSpaceWebSocketHandler
        implements WebSocketHandler {

    private final WatchSpaceRoomManager roomManager;
    private final PresenceEventFactory presenceEventFactory;
    private final ObjectMapper objectMapper;
    private final ChatEventFactory chatEventFactory;
    private final AiTriviaService aiTriviaService;
    private final AiTriviaEventFactory aiTriviaEventFactory;

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        Long watchSpaceId =
                extractWatchSpaceId(session);

        return session.getHandshakeInfo()
                .getPrincipal()
                .flatMap(principal -> {

                    JwtAuthenticationToken authentication =
                            (JwtAuthenticationToken) principal;

                    Long userId =
                            Long.parseLong(
                                    Objects.requireNonNull(
                                            authentication
                                                    .getToken()
                                                    .getSubject()
                                    )
                            );

                    WatchSpaceConnection connection =
                            new WatchSpaceConnection(
                                    userId,
                                    session
                            );

                    roomManager.addSession(
                            watchSpaceId,
                            connection
                    );

                    WebSocketEvent joinedEvent =
                            presenceEventFactory
                                    .createJoinedEvent(userId);

                    return roomManager
                            .broadcast(
                                    watchSpaceId,
                                    joinedEvent
                            )
                            .then(

                                    session.receive()

                                            .flatMap(message -> {

                                                String payload =
                                                        message.getPayloadAsText();

                                                try {

                                                    WebSocketEvent event =
                                                            objectMapper.readValue(
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
                                                                .map(trivia ->
                                                                        aiTriviaEventFactory
                                                                                .createTriviaEvent(trivia)
                                                                )
                                                                .flatMap(triviaEvent ->
                                                                        roomManager.broadcast(
                                                                                watchSpaceId,
                                                                                triviaEvent
                                                                        )
                                                                );
                                                    }
                                                    // CHAT MESSAGE
                                                    if ("chat.message".equals(event.getType())) {

                                                        Object messageData =
                                                                event.getData()
                                                                        .get("message");

                                                        if (messageData == null) {
                                                            return Mono.empty();
                                                        }

                                                        String chatMessage =
                                                                messageData
                                                                        .toString()
                                                                        .trim();

                                                        if (chatMessage.isEmpty()) {
                                                            return Mono.empty();
                                                        }

                                                        WebSocketEvent chatEvent =
                                                                chatEventFactory
                                                                        .createChatMessage(
                                                                                userId,
                                                                                chatMessage
                                                                        );

                                                        return roomManager
                                                                .broadcast(
                                                                        watchSpaceId,
                                                                        chatEvent
                                                                );
                                                    }

                                                    // Ignore unknown event types
                                                    return Mono.empty();

                                                } catch (JacksonException exception) {

                                                    System.out.println(
                                                            "Invalid WebSocket event: "
                                                                    + exception.getMessage()
                                                    );

                                                    return Mono.empty();
                                                }
                                            })

                                            /*
                                             * When the WebSocket receive stream
                                             * completes, remove the connection
                                             * and broadcast LEFT.
                                             */
                                            .then(
                                                    Mono.defer(() -> {

                                                        roomManager.removeSession(
                                                                watchSpaceId,
                                                                connection
                                                        );

                                                        WebSocketEvent leftEvent =
                                                                presenceEventFactory
                                                                        .createLeftEvent(
                                                                                userId
                                                                        );

                                                        return roomManager
                                                                .broadcast(
                                                                        watchSpaceId,
                                                                        leftEvent
                                                                );
                                                    })
                                            )
                            );
                });
    }

    private Long extractWatchSpaceId(
            WebSocketSession session
    ) {

        String path =
                session.getHandshakeInfo()
                        .getUri()
                        .getPath();

        String[] parts = path.split("/");

        return Long.parseLong(
                parts[parts.length - 1]
        );
    }
}