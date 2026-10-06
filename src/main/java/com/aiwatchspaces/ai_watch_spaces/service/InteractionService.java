package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.dto.CreateInteractionRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.InteractionResponse;
import com.aiwatchspaces.ai_watch_spaces.entity.Interaction;
import com.aiwatchspaces.ai_watch_spaces.entity.Title;
import com.aiwatchspaces.ai_watch_spaces.exception.TitleNotFoundException;
import com.aiwatchspaces.ai_watch_spaces.repository.InteractionRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.TitleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final TitleRepository titleRepository;

    @Transactional
    public InteractionResponse recordInteraction(
            Long userId,
            CreateInteractionRequest request
    ) {
        Title title = titleRepository.findById(request.titleId())
                .orElseThrow(() ->
                        new TitleNotFoundException(
                                "Title not found with id: " + request.titleId()
                        ));

        if (request.watchedSeconds() > title.getDurationSeconds()) {
            throw new IllegalArgumentException(
                    "Watched seconds cannot exceed title duration"
            );
        }

        if (Boolean.TRUE.equals(request.completed())
                && request.watchedSeconds() < title.getDurationSeconds()) {
            throw new IllegalArgumentException(
                    "A completed interaction must have watched the full title"
            );
        }

        Interaction interaction = new Interaction(
                userId,
                request.titleId(),
                request.watchedSeconds(),
                request.completed(),
                request.rating()
        );

        Interaction savedInteraction =
                interactionRepository.save(interaction);

        return toResponse(savedInteraction);
    }

    private InteractionResponse toResponse(Interaction interaction) {
        return new InteractionResponse(
                interaction.getId(),
                interaction.getUserId(),
                interaction.getTitleId(),
                interaction.getWatchedSeconds(),
                interaction.getCompleted(),
                interaction.getRating(),
                interaction.getCreatedAt()
        );
    }
}