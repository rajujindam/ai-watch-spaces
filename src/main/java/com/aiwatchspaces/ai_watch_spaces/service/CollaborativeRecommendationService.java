package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.entity.Interaction;
import com.aiwatchspaces.ai_watch_spaces.repository.InteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollaborativeRecommendationService {

    private final InteractionRepository interactionRepository;

    public Map<Long, Double> calculateUserSimilarities(Long userId) {

        Set<Long> targetUserTitles =
                interactionRepository.findByUserId(userId)
                        .stream()
                        .map(Interaction::getTitleId)
                        .collect(Collectors.toSet());

        List<Interaction> otherUserInteractions =
                interactionRepository.findByUserIdNot(userId);

        Map<Long, Set<Long>> titlesByUser =
                otherUserInteractions.stream()
                        .collect(Collectors.groupingBy(
                                Interaction::getUserId,
                                Collectors.mapping(
                                        Interaction::getTitleId,
                                        Collectors.toSet()
                                )
                        ));

        return titlesByUser.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> calculateJaccardSimilarity(
                                targetUserTitles,
                                entry.getValue()
                        )
                ));
    }

    private double calculateJaccardSimilarity(
            Set<Long> first,
            Set<Long> second
    ) {
        if (first.isEmpty() && second.isEmpty()) {
            return 0.0;
        }

        Set<Long> intersection = new HashSet<>(first);
        intersection.retainAll(second);

        Set<Long> union = new HashSet<>(first);
        union.addAll(second);
        return (double) intersection.size() / union.size();
    }


    public Map<Long, Double> calculateCollaborativeScores(Long userId) {

        Map<Long, Double> userSimilarities =
                calculateUserSimilarities(userId);

        Set<Long> interactedTitleIds =
                interactionRepository.findByUserId(userId)
                        .stream()
                        .map(Interaction::getTitleId)
                        .collect(Collectors.toSet());

        List<Interaction> otherUserInteractions =
                interactionRepository.findByUserIdNot(userId);

        Map<Long, Double> titleScores = new HashMap<>();

        for (Interaction interaction : otherUserInteractions) {

            Long otherUserId = interaction.getUserId();

            double similarity =
                    userSimilarities.getOrDefault(
                            otherUserId,
                            0.0
                    );

            if (similarity <= 0) {
                continue;
            }

            Long titleId = interaction.getTitleId();

            if (interactedTitleIds.contains(titleId)) {
                continue;
            }

            titleScores.merge(
                    titleId,
                    similarity,
                    Double::sum
            );
        }

        return titleScores;
    }
}