package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.entity.Interaction;
import com.aiwatchspaces.ai_watch_spaces.entity.Title;
import com.aiwatchspaces.ai_watch_spaces.repository.InteractionRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.TitleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GenrePreferenceService {

    private final InteractionRepository interactionRepository;
    private final TitleRepository titleRepository;

    @Transactional(readOnly = true)
    public Map<String, Double> calculateGenrePreferences(Long userId) {

        List<Interaction> interactions =
                interactionRepository.findByUserId(userId);

        Map<String, Double> genreScores = new HashMap<>();

        for (Interaction interaction : interactions) {

            Title title = titleRepository.findById(interaction.getTitleId())
                    .orElse(null);

            if (title == null) {
                continue;
            }

            double interactionWeight =
                    calculateInteractionWeight(interaction);

            for (String genre : title.getGenres()) {

                genreScores.merge(
                        genre,
                        interactionWeight,
                        Double::sum
                );
            }
        }

        return genreScores;
    }

    private double calculateInteractionWeight(Interaction interaction) {

        double ratingScore = 0.0;

        if (interaction.getRating() != null) {
            ratingScore =
                    (interaction.getRating() - 1) / 4.0;
        }

        double completionScore =
                Boolean.TRUE.equals(interaction.getCompleted())
                        ? 1.0
                        : 0.0;

        return (0.7 * ratingScore)
                + (0.3 * completionScore);
    }
}