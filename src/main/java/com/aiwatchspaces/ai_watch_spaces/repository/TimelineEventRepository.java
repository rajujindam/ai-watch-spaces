package com.aiwatchspaces.ai_watch_spaces.repository;

import com.aiwatchspaces.ai_watch_spaces.entity.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimelineEventRepository
        extends JpaRepository<TimelineEvent, Long> {

    List<TimelineEvent>
    findByTitle_IdAndTimestampSecondsBetweenOrderByTimestampSecondsAsc(
            Long titleId,
            Integer startSeconds,
            Integer endSeconds
    );
}