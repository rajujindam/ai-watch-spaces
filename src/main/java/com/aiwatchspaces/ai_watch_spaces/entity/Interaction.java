package com.aiwatchspaces.ai_watch_spaces.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "interactions")
@Getter
@Setter
@NoArgsConstructor
public class Interaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title_id", nullable = false)
    private Long titleId;

    @Min(0)
    @Column(name = "watched_seconds", nullable = false)
    private Integer watchedSeconds;

    @Column(nullable = false)
    private Boolean completed = false;

    @Min(1)
    @Max(5)
    private Integer rating;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Interaction(
            Long userId,
            Long titleId,
            Integer watchedSeconds,
            Boolean completed,
            Integer rating
    ) {
        this.userId = userId;
        this.titleId = titleId;
        this.watchedSeconds = watchedSeconds;
        this.completed = completed;
        this.rating = rating;
        this.createdAt = Instant.now();
    }
}