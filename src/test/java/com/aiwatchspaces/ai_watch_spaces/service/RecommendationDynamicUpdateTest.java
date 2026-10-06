package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.entity.Interaction;
import com.aiwatchspaces.ai_watch_spaces.entity.Title;
import com.aiwatchspaces.ai_watch_spaces.repository.InteractionRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.TitleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationDynamicUpdateTest {

    @Mock
    private InteractionRepository interactionRepository;

    @Mock
    private TitleRepository titleRepository;

    @Test
    void recommendationsShouldUpdateAfterNewInteraction() {

        // ---------- TITLES ----------

        Map<Long, Title> titles = new HashMap<>();

        titles.put(1L, createTitle(
                1L,
                "Interstellar",
                Set.of("Sci-Fi", "Drama")
        ));

        titles.put(2L, createTitle(
                2L,
                "Inception",
                Set.of("Sci-Fi", "Thriller")
        ));

        titles.put(3L, createTitle(
                3L,
                "Dune",
                Set.of("Sci-Fi", "Adventure")
        ));

        titles.put(4L, createTitle(
                4L,
                "The Martian",
                Set.of("Sci-Fi", "Adventure")
        ));

        titles.put(5L, createTitle(
                5L,
                "The Notebook",
                Set.of("Romance", "Drama")
        ));

        titles.put(6L, createTitle(
                6L,
                "Titanic",
                Set.of("Romance", "Drama")
        ));


        // ---------- INITIAL USER INTERACTIONS ----------

        List<Interaction> userInteractions = new ArrayList<>();

        userInteractions.add(
                interaction(1L, 1L, 7200, true, 5)
        );

        userInteractions.add(
                interaction(1L, 2L, 8880, true, 5)
        );

        userInteractions.add(
                interaction(1L, 4L, 8640, true, 4)
        );


        // ---------- OTHER USER INTERACTIONS ----------

        List<Interaction> otherUserInteractions = List.of(

                interaction(2L, 1L, 7200, true, 5),
                interaction(2L, 2L, 8880, true, 5),
                interaction(2L, 3L, 9300, true, 5),

                interaction(3L, 5L, 7380, true, 5),
                interaction(3L, 6L, 7200, true, 5)
        );


        // ---------- REPOSITORY BEHAVIOUR ----------

        when(interactionRepository.findByUserId(1L))
                .thenAnswer(invocation -> userInteractions);

        when(interactionRepository.findByUserIdNot(1L))
                .thenReturn(otherUserInteractions);

        when(titleRepository.findAll())
                .thenReturn(new ArrayList<>(titles.values()));

        when(titleRepository.findById(org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(invocation -> {

                    Long titleId = invocation.getArgument(0);

                    return Optional.ofNullable(titles.get(titleId));
                });


        // ---------- CREATE REAL RECOMMENDATION SERVICES ----------

        GenrePreferenceService genrePreferenceService =
                new GenrePreferenceService(
                        interactionRepository,
                        titleRepository
                );

        ContentRecommendationService contentRecommendationService =
                new ContentRecommendationService(
                        genrePreferenceService,
                        interactionRepository,
                        titleRepository
                );

        CollaborativeRecommendationService collaborativeRecommendationService =
                new CollaborativeRecommendationService(
                        interactionRepository
                );

        RecommendationService recommendationService =
                new RecommendationService(
                        contentRecommendationService,
                        collaborativeRecommendationService,
                        titleRepository
                );


        // ---------- BEFORE NEW INTERACTION ----------

        List<RecommendationService.RecommendationScore> before =
                recommendationService.recommend(1L, 5);


        boolean romanceRecommendedBefore =
                before.stream()
                        .anyMatch(recommendation ->
                                recommendation.title()
                                        .getGenres()
                                        .contains("Romance")
                        );


        // ---------- ADD NEW INTERACTION ----------

        // User 1 watches The Notebook and gives it 5 stars.

        userInteractions.add(
                interaction(1L, 5L, 7380, true, 5)
        );


        // ---------- AFTER NEW INTERACTION ----------

        List<RecommendationService.RecommendationScore> after =
                recommendationService.recommend(1L, 5);


        // ---------- VERIFY ----------

        boolean notebookRecommendedAfter =
                after.stream()
                        .anyMatch(recommendation ->
                                recommendation.title()
                                        .getId()
                                        .equals(5L)
                        );


        // The title should no longer be recommended because
        // the user has already interacted with it.

        assertTrue(
                !notebookRecommendedAfter,
                "A title already interacted with should not be recommended"
        );


        // Before the interaction, romance titles could be recommended.
        // After watching The Notebook, that title becomes part of
        // the user's interaction history and is excluded.

        assertTrue(
                romanceRecommendedBefore,
                "Romance recommendation should exist before the new interaction"
        );
    }


    // ---------- TEST DATA HELPERS ----------

    private Title createTitle(
            Long id,
            String name,
            Set<String> genres
    ) {

        Title title = new Title();

        title.setId(id);
        title.setName(name);
        title.setGenres(genres);
        title.setDescription("Test title");
        title.setDurationSeconds(10000);

        return title;
    }


    private Interaction interaction(
            Long userId,
            Long titleId,
            Integer watchedSeconds,
            Boolean completed,
            Integer rating
    ) {

        return new Interaction(
                userId,
                titleId,
                watchedSeconds,
                completed,
                rating
        );
    }
}