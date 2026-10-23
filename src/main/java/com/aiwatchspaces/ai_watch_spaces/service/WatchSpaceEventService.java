package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpaceEvent;
import com.aiwatchspaces.ai_watch_spaces.repository.WatchSpaceEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
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

    public List<WatchSpaceEvent> getRecentChatMessages(Long watchSpaceId) {
        List<WatchSpaceEvent> messages =
                watchSpaceEventRepository
                        .findTop50ByWatchSpaceIdAndEventTypeOrderByOccurredAtDesc(
                                watchSpaceId,
                                "CHAT_MESSAGE"
                        );

        Collections.reverse(messages);
        return messages;
    }

}