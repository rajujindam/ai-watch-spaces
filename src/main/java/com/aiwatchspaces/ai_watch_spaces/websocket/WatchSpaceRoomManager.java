package com.aiwatchspaces.ai_watch_spaces.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WatchSpaceRoomManager {

    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<
            Long,
            Set<WatchSpaceConnection>
            > rooms = new ConcurrentHashMap<>();


    public void addSession(
            Long watchSpaceId,
            WatchSpaceConnection connection
    ) {
        rooms.compute(watchSpaceId, (id, connections) -> {
            if (connections == null) {
                connections = ConcurrentHashMap.newKeySet();
            }

            connections.add(connection);
            return connections;
        });
    }



    public void removeSession(
            Long watchSpaceId,
            WatchSpaceConnection connection
    ) {
        rooms.computeIfPresent(watchSpaceId, (id, connections) -> {
            connections.remove(connection);

            return connections.isEmpty() ? null : connections;
        });
    }


    public Set<WatchSpaceConnection> getConnections(
            Long watchSpaceId
    ) {

        return rooms.getOrDefault(
                watchSpaceId,
                Set.of()
        );
    }

    public Mono<Void> broadcast(
            Long watchSpaceId,
            WebSocketEvent event
    ) {

        String json;

        try {
            json = objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            return Mono.error(exception);
        }

        Set<WatchSpaceConnection> connections =
                rooms.getOrDefault(
                        watchSpaceId,
                        Set.of()
                );

        return Flux.fromIterable(connections)
                .flatMap(connection ->
                        connection.getSession().send(
                                Mono.just(
                                        connection
                                                .getSession()
                                                .textMessage(json)
                                )
                        )
                )
                .then();
    }

    public boolean hasActiveConnection(
            Long watchSpaceId,
            Long userId
    ) {
        Set<WatchSpaceConnection> connections =
                rooms.get(watchSpaceId);

        if (connections == null) {
            return false;
        }

        return connections.stream()
                .anyMatch(connection ->
                        connection.getUserId().equals(userId)
                                && connection.getSession().isOpen()
                );
    }

    public Mono<Void> sendToConnection(
            WatchSpaceConnection connection,
            WebSocketEvent event
    ) {
        if (!connection.getSession().isOpen()) {
            return Mono.empty();
        }

        try {
            String json = objectMapper.writeValueAsString(event);

            return connection.getSession().send(
                    Mono.just(
                            connection.getSession().textMessage(json)
                    )
            );
        } catch (JacksonException exception) {
            return Mono.error(exception);
        }
    }
    public Mono<Void> broadcastExceptUser(
            Long watchSpaceId,
            Long excludedUserId,
            WebSocketEvent event
    ) {
        String json;

        try {
            json = objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            return Mono.error(exception);
        }

        Set<WatchSpaceConnection> connections =
                rooms.getOrDefault(watchSpaceId, Set.of());

        return Flux.fromIterable(connections)
                .filter(connection ->
                        !connection.getUserId().equals(excludedUserId))
                .flatMap(connection ->
                        connection.getSession().send(
                                Mono.just(
                                        connection.getSession().textMessage(json)
                                )
                        )
                )
                .then();
    }

}