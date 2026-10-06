package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.entity.Title;
import com.aiwatchspaces.ai_watch_spaces.repository.TitleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ContentRecommendationService contentRecommendationService;
    private final CollaborativeRecommendationService collaborativeRecommendationService;
    private final TitleRepository titleRepository;

    public List<RecommendationScore> recommend(Long userId, int limit) {

        List<ContentRecommendationService.TitleScore> contentRecommendations =
                contentRecommendationService.recommend(
                        userId,
                        Integer.MAX_VALUE
                );

        Map<Long, Double> contentScores =
                contentRecommendations.stream()
                        .collect(Collectors.toMap(
                                recommendation ->
                                        recommendation.title().getId(),
                                ContentRecommendationService.TitleScore::score
                        ));

        Map<Long, Double> collaborativeScores =
                collaborativeRecommendationService
                        .calculateCollaborativeScores(userId);

        Set<Long> candidateTitleIds = new HashSet<>();

        candidateTitleIds.addAll(contentScores.keySet());
        candidateTitleIds.addAll(collaborativeScores.keySet());

        double maxContentScore =
                contentScores.values()
                        .stream()
                        .mapToDouble(Double::doubleValue)
                        .max()
                        .orElse(0.0);

        double maxCollaborativeScore =
                collaborativeScores.values()
                        .stream()
                        .mapToDouble(Double::doubleValue)
                        .max()
                        .orElse(0.0);

        List<RecommendationScore> recommendations =
                candidateTitleIds.stream()
                        .map(titleId -> {

                            Title title =
                                    titleRepository.findById(titleId)
                                            .orElse(null);

                            if (title == null) {
                                return null;
                            }

                            double contentScore =
                                    contentScores.getOrDefault(
                                            titleId,
                                            0.0
                                    );

                            double collaborativeScore =
                                    collaborativeScores.getOrDefault(
                                            titleId,
                                            0.0
                                    );

                            double normalizedContentScore =
                                    normalize(
                                            contentScore,
                                            maxContentScore
                                    );

                            double normalizedCollaborativeScore =
                                    normalize(
                                            collaborativeScore,
                                            maxCollaborativeScore
                                    );

                            double hybridScore =
                                    (0.6 * normalizedContentScore)
                                            +
                                            (0.4 * normalizedCollaborativeScore);

                            return new RecommendationScore(
                                    title,
                                    normalizedContentScore,
                                    normalizedCollaborativeScore,
                                    hybridScore
                            );
                        })
                        .filter(Objects::nonNull)
                        .sorted(
                                Comparator.comparingDouble(
                                        RecommendationScore::hybridScore
                                ).reversed()
                        )
                        .limit(limit)
                        .toList();

        return recommendations;
    }

    private double normalize(
            double score,
            double maximum
    ) {
        if (maximum <= 0) {
            return 0.0;
        }

        return score / maximum;
    }

    public record RecommendationScore(
            Title title,
            double contentScore,
            double collaborativeScore,
            double hybridScore
    ) {
    }
}