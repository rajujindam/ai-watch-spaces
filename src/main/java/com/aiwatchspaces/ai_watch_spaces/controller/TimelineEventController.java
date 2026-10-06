
package com.aiwatchspaces.ai_watch_spaces.controller;

import com.aiwatchspaces.ai_watch_spaces.dto.CreateTimelineEventRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.TimelineEventResponse;
import com.aiwatchspaces.ai_watch_spaces.service.TimelineEventService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/titles/{titleId}/timeline-events")
@RequiredArgsConstructor
public class TimelineEventController {

    private final TimelineEventService timelineEventService;

    @PostMapping
    public ResponseEntity<TimelineEventResponse> createEvent(
            @PathVariable Long titleId,
            @Valid @RequestBody CreateTimelineEventRequest request
    ) {
        TimelineEventResponse response =
                timelineEventService.createEvent(titleId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<TimelineEventResponse>> getEventsInRange(
            @PathVariable Long titleId,
            @RequestParam Integer startSeconds,
            @RequestParam Integer endSeconds
    ) {
        return ResponseEntity.ok(
                timelineEventService.getEventsInRange(
                        titleId,
                        startSeconds,
                        endSeconds
                )
        );
    }
}