
package com.aiwatchspaces.ai_watch_spaces.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "watch_space_participants",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"watch_space_id", "user_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class WatchSpaceParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "watch_space_id", nullable = false)
    private WatchSpace watchSpace;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    public WatchSpaceParticipant(WatchSpace watchSpace, Long userId) {
        this.watchSpace = watchSpace;
        this.userId = userId;
        this.joinedAt = LocalDateTime.now();
    }
}