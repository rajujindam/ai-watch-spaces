package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.entity.Title;
import com.aiwatchspaces.ai_watch_spaces.repository.InteractionRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.TitleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentRecommendationService {

    private final GenrePreferenceService genrePreferenceService;
    private final InteractionRepository interactionRepository;
    private final TitleRepository titleRepository;

    @Transactional(readOnly = true)
    public List<TitleScore> recommend(Long userId, int limit) {

        Map<String, Double> genrePreferences =
                genrePreferenceService.calculateGenrePreferences(userId);

        Set<Long> interactedTitleIds =
                interactionRepository.findByUserId(userId)
                        .stream()
                        .map(interaction ->
                                interaction.getTitleId())
                        .collect(Collectors.toSet());

        return titleRepository.findAll()
                .stream()
                .filter(title ->
                        !interactedTitleIds.contains(title.getId()))
                .map(title ->
                        new TitleScore(
                                title,
                                calculateScore(
                                        title,
                                        genrePreferences
                                )
                        ))
                .filter(titleScore ->
                        titleScore.score() > 0)
                .sorted(
                        Comparator.comparingDouble(
                                TitleScore::score
                        ).reversed()
                )
                .limit(limit)
                .toList();
    }

    private double calculateScore(
            Title title,
            Map<String, Double> genrePreferences
    ) {
        return title.getGenres()
                .stream()
                .mapToDouble(genre ->
                        genrePreferences.getOrDefault(
                                genre,
                                0.0
                        ))
                .sum();
    }

    public record TitleScore(
            Title title,
            double score
    ) {
    }
}