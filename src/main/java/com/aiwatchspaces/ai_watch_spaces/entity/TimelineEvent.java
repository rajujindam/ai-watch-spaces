
package com.aiwatchspaces.ai_watch_spaces.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(
        name = "timeline_events",
        indexes = {
                @Index(
                        name = "idx_timeline_title_timestamp",
                        columnList = "title_id, timestamp_seconds"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "title_id", nullable = false)
    private Title title;

    @NotNull
    @PositiveOrZero
    @Column(name = "timestamp_seconds", nullable = false)
    private Integer timestampSeconds;

    @NotBlank
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

    public TimelineEvent(
            Title title,
            Integer timestampSeconds,
            String eventType,
            Map<String, Object> payload
    ) {
        this.title = title;
        this.timestampSeconds = timestampSeconds;
        this.eventType = eventType;
        this.payload = payload;
    }
}