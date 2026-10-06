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

    public void addSession(Long watchSpaceId, WatchSpaceConnection connection) {

        rooms.computeIfAbsent(
                watchSpaceId,
                id -> ConcurrentHashMap.newKeySet()
        ).add(connection);
    }

    public void removeSession(Long watchSpaceId, WatchSpaceConnection connection) {
        Set<WatchSpaceConnection> connections = rooms.get(watchSpaceId);

        if (connections == null) {
            return;
        }

        connections.remove(connection);

        if (connections.isEmpty()) {
            rooms.remove(watchSpaceId);
        }
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
}