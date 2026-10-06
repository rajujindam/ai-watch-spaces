
package com.aiwatchspaces.ai_watch_spaces.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    public WatchSpace(String name, Long hostId, Long titleId) {
        this.name = name;
        this.hostId = hostId;
        this.titleId = titleId;
        this.createdAt = LocalDateTime.now();
    }
}