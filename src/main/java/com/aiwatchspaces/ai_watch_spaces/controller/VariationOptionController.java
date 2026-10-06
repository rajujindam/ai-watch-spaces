package com.aiwatchspaces.ai_watch_spaces.controller;

import com.aiwatchspaces.ai_watch_spaces.dto.CreateVariationOptionRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.VariationOptionResponse;
import com.aiwatchspaces.ai_watch_spaces.service.VariationOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/timeline-events/{timelineEventId}/variation-options")
@RequiredArgsConstructor
public class VariationOptionController {

    private final VariationOptionService variationOptionService;

    @PostMapping
    public ResponseEntity<VariationOptionResponse> createOption(
            @PathVariable Long timelineEventId,
            @Valid @RequestBody CreateVariationOptionRequest request
    ) {
        VariationOptionResponse response =
                variationOptionService.createOption(
                        timelineEventId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<VariationOptionResponse>> getOptions(
            @PathVariable Long timelineEventId
    ) {
        List<VariationOptionResponse> response =
                variationOptionService.getOptions(timelineEventId);

        return ResponseEntity.ok(response);
    }
}