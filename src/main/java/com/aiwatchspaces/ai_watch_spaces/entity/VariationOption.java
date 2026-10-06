package com.aiwatchspaces.ai_watch_spaces.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "variation_options")
@Getter
@Setter
@NoArgsConstructor
public class VariationOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timeline_event_id", nullable = false)
    private TimelineEvent timelineEvent;

    @NotBlank
    @Column(nullable = false)
    private String label;

    @Column(name = "asset_ref")
    private String assetRef;

    @NotNull
    @PositiveOrZero
    @Column(name = "vote_count", nullable = false)
    private Integer voteCount = 0;

    public VariationOption(
            TimelineEvent timelineEvent,
            String label,
            String assetRef
    ) {
        this.timelineEvent = timelineEvent;
        this.label = label;
        this.assetRef = assetRef;
        this.voteCount = 0;
    }
}