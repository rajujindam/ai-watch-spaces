package com.aiwatchspaces.ai_watch_spaces.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "watch_space_events",
        indexes = {
                @Index(
                        name = "idx_watch_space_event_room_time",
                        columnList = "watch_space_id, occurred_at"
                ),
                @Index(
                        name = "idx_watch_space_event_type",
                        columnList = "event_type"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class WatchSpaceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "watch_space_id", nullable = false)
    private Long watchSpaceId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    public WatchSpaceEvent(
            Long watchSpaceId,
            Long userId,
            String eventType,
            Map<String, Object> metadata
    ) {
        this.watchSpaceId = watchSpaceId;
        this.userId = userId;
        this.eventType = eventType;
        this.occurredAt = Instant.now();

        if (metadata != null) {
            this.metadata = metadata;
        }
    }
}