package com.aiwatchspaces.ai_watch_spaces.repository;

import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpaceEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchSpaceEventRepository
        extends JpaRepository<WatchSpaceEvent, Long> {

    List<WatchSpaceEvent> findByWatchSpaceIdOrderByOccurredAtAsc(
            Long watchSpaceId
    );
}