
package com.aiwatchspaces.ai_watch_spaces.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "watch_spaces")
@Getter
@Setter
@NoArgsConstructor

public class WatchSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long hostId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long titleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WatchSpaceStatus status;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiVerbosity aiVerbosity;

    @Column(nullable = false)
    private Boolean votingEnabled;

    @Column
    private LocalDateTime endedAt;

    @Column(nullable = false)
    private Boolean locked = false;

    @Column(unique = true, length = 36)
    private String inviteCode;

    public WatchSpace(
            String name,
            Long hostId,
            Long titleId,
            Integer maxParticipants,
            AiVerbosity aiVerbosity,
            Boolean votingEnabled
    ) {
        this.name = name;
        this.hostId = hostId;
        this.titleId = titleId;
        this.status = WatchSpaceStatus.SCHEDULED;
        this.maxParticipants = maxParticipants;
        this.aiVerbosity = aiVerbosity;
        this.votingEnabled = votingEnabled;
        this.createdAt = LocalDateTime.now();
        this.inviteCode = UUID.randomUUID().toString();
    }
}