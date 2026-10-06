package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.dto.CreateTitleRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.TitleResponse;
import com.aiwatchspaces.ai_watch_spaces.entity.Title;
import com.aiwatchspaces.ai_watch_spaces.exception.TitleNotFoundException;
import com.aiwatchspaces.ai_watch_spaces.repository.TitleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TitleService {

    private final TitleRepository titleRepository;

    public TitleResponse createTitle(CreateTitleRequest request) {

        Title title = new Title(
                request.getName(),
                request.getDescription(),
                request.getDurationSeconds()
        );

        Title savedTitle = titleRepository.save(title);

        return new TitleResponse(
                savedTitle.getId(),
                savedTitle.getName(),
                savedTitle.getDescription(),
                savedTitle.getDurationSeconds()
        );
    }

    public TitleResponse getTitleById(Long id) {

        Title title = titleRepository.findById(id)
                .orElseThrow(() -> new TitleNotFoundException(
                        "Title not found with id: " + id
                ));

        return new TitleResponse(
                title.getId(),
                title.getName(),
                title.getDescription(),
                title.getDurationSeconds()
        );
    }
}