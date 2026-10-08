package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpaceEvent;
import com.aiwatchspaces.ai_watch_spaces.repository.WatchSpaceEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class WatchSpaceEventService {

    private final WatchSpaceEventRepository watchSpaceEventRepository;

    public WatchSpaceEvent recordEvent(
            Long watchSpaceId,
            Long userId,
            String eventType,
            Map<String, Object> metadata
    ) {

        WatchSpaceEvent event = new WatchSpaceEvent(
                watchSpaceId,
                userId,
                eventType,
                metadata
        );

        return watchSpaceEventRepository.save(event);
    }
}