package com.aiwatchspaces.ai_watch_spaces.controller;

import com.aiwatchspaces.ai_watch_spaces.dto.CreateTitleRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.TitleResponse;
import com.aiwatchspaces.ai_watch_spaces.service.TitleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/titles")
@RequiredArgsConstructor
public class TitleController {

    private final TitleService titleService;

    @PostMapping
    public ResponseEntity<TitleResponse> createTitle(
            @Valid @RequestBody CreateTitleRequest request) {

        TitleResponse response = titleService.createTitle(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TitleResponse> getTitleById(
            @PathVariable Long id) {

        TitleResponse response = titleService.getTitleById(id);

        return ResponseEntity.ok(response);
    }
}