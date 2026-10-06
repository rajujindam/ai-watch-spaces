
package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.dto.CreateTimelineEventRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.TimelineEventResponse;
import com.aiwatchspaces.ai_watch_spaces.entity.Title;
import com.aiwatchspaces.ai_watch_spaces.entity.TimelineEvent;
import com.aiwatchspaces.ai_watch_spaces.exception.InvalidTimelineRequestException;
import com.aiwatchspaces.ai_watch_spaces.exception.TitleNotFoundException;
import com.aiwatchspaces.ai_watch_spaces.repository.TitleRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.TimelineEventRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimelineEventService {

    private final TitleRepository titleRepository;
    private final TimelineEventRepository timelineEventRepository;

    @Transactional
    public TimelineEventResponse createEvent(
            Long titleId,
            CreateTimelineEventRequest request
    ) {
        Title title = titleRepository.findById(titleId)
                .orElseThrow(() ->
                        new TitleNotFoundException(
                                "Title not found with id: " + titleId
                        )
                );

        if (request.getTimestampSeconds() > title.getDurationSeconds()) {
            throw new InvalidTimelineRequestException(
                    "Event timestamp cannot exceed title duration"
            );
        }

        TimelineEvent event = new TimelineEvent(
                title,
                request.getTimestampSeconds(),
                request.getEventType(),
                request.getPayload()
        );

        TimelineEvent savedEvent =
                timelineEventRepository.save(event);

        return toResponse(savedEvent);
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> getEventsInRange(
            Long titleId,
            Integer startSeconds,
            Integer endSeconds
    ) {
        Title title = titleRepository.findById(titleId)
                .orElseThrow(() ->
                        new TitleNotFoundException(
                                "Title not found with id: " + titleId
                        )
                );

        if (startSeconds < 0 || endSeconds < 0) {
            throw new InvalidTimelineRequestException(
                    "Time range cannot contain negative values"
            );
        }

        if (startSeconds > endSeconds) {
            throw new InvalidTimelineRequestException(
                    "Start time cannot be greater than end time"
            );
        }

        if (endSeconds > title.getDurationSeconds()) {
            throw new InvalidTimelineRequestException(
                    "End time cannot exceed title duration"
            );
        }

        return timelineEventRepository
                .findByTitle_IdAndTimestampSecondsBetweenOrderByTimestampSecondsAsc(
                        titleId,
                        startSeconds,
                        endSeconds
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TimelineEventResponse toResponse(TimelineEvent event) {
        return new TimelineEventResponse(
                event.getId(),
                event.getTitle().getId(),
                event.getTimestampSeconds(),
                event.getEventType(),
                event.getPayload()
        );
    }
}