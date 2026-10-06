package com.aiwatchspaces.ai_watch_spaces.ai;

import com.aiwatchspaces.ai_watch_spaces.entity.TimelineEvent;
import com.aiwatchspaces.ai_watch_spaces.repository.TimelineEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimelineRetrievalService {

    private final TimelineEventRepository timelineEventRepository;

    public List<TimelineEvent> getEvents(
            Long titleId,
            Integer startSeconds,
            Integer endSeconds
    ) {
        return timelineEventRepository
                .findByTitle_IdAndTimestampSecondsBetweenOrderByTimestampSecondsAsc(
                        titleId,
                        startSeconds,
                        endSeconds
                );
    }
}