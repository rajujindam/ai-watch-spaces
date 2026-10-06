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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationEvaluatorTest {

    @Mock
    private InteractionRepository interactionRepository;

    @Mock
    private TitleRepository titleRepository;

    @Test
    void precisionAt5ShouldMeetPrdTarget() {

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
                "Blade Runner",
                Set.of("Sci-Fi", "Thriller")
        ));

        titles.put(6L, createTitle(
                6L,
                "Arrival",
                Set.of("Sci-Fi", "Drama")
        ));

        titles.put(7L, createTitle(
                7L,
                "The Notebook",
                Set.of("Romance", "Drama")
        ));

        titles.put(8L, createTitle(
                8L,
                "The Hangover",
                Set.of("Comedy")
        ));

        titles.put(9L, createTitle(
                9L,
                "John Wick",
                Set.of("Action", "Thriller")
        ));

        titles.put(10L, createTitle(
                10L,
                "The Dark Knight",
                Set.of("Action", "Crime", "Drama")
        ));


        // ---------- USER 1 KNOWN INTERACTIONS ----------
        //
        // These are the only interactions the recommender
        // is allowed to see for User 1.

        List<Interaction> user1Interactions = List.of(

                interaction(
                        1L,
                        1L,
                        7200,
                        true,
                        5
                ),

                interaction(
                        1L,
                        2L,
                        8880,
                        true,
                        5
                ),

                interaction(
                        1L,
                        4L,
                        8640,
                        true,
                        4
                )
        );


        // ---------- OTHER USERS ----------
        //
        // User 2 is very similar to User 1 and has interacted
        // with the three titles that we hide from User 1.

        List<Interaction> otherUserInteractions = List.of(

                // User 2
                interaction(2L, 1L, 7200, true, 5),
                interaction(2L, 2L, 8880, true, 4),
                interaction(2L, 4L, 8640, true, 5),

                // These are the held-out titles
                interaction(2L, 3L, 9300, true, 5),
                interaction(2L, 5L, 9000, true, 5),
                interaction(2L, 6L, 7500, true, 5),

                // User 3
                interaction(3L, 1L, 7200, true, 4),
                interaction(3L, 9L, 6000, true, 5),
                interaction(3L, 10L, 9120, true, 5),

                // User 4
                interaction(4L, 7L, 7380, true, 5),
                interaction(4L, 8L, 6000, true, 5)
        );


        // ---------- MOCK REPOSITORIES ----------

        when(interactionRepository.findByUserId(1L))
                .thenReturn(user1Interactions);

        when(interactionRepository.findByUserIdNot(1L))
                .thenReturn(otherUserInteractions);

        when(titleRepository.findAll())
                .thenReturn(new ArrayList<>(titles.values()));

        when(titleRepository.findById(anyLong()))
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

        RecommendationEvaluator evaluator =
                new RecommendationEvaluator(
                        recommendationService
                );


        // ---------- HELD-OUT GROUND TRUTH ----------

        Set<Long> heldOutTitleIds = Set.of(
                3L,
                5L,
                6L
        );


        // ---------- CALCULATE PRECISION@5 ----------

        double precisionAt5 =
                evaluator.calculatePrecisionAt5(
                        1L,
                        heldOutTitleIds
                );


        // ---------- ASSERT PRD TARGET ----------

        assertEquals(
                0.6,
                precisionAt5,
                0.0001
        );

        assertTrue(
                precisionAt5 >= 0.6,
                "Precision@5 must be at least 0.6"
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