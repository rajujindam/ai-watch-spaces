package com.aiwatchspaces.ai_watch_spaces.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@Import(AiContentEngineTest.TestConfig.class)
class AiContentEngineTest {
    @TestConfiguration
    @Primary
    static class TestConfig {

        @Bean
        AiProvider aiProvider() {
            return new TestAiProvider();
        }
    }

    @Autowired
    private AiContentEngine aiContentEngine;

    @Test
    void shouldBuildGroundedContextFromDatabase() {

        String context =
                aiContentEngine.buildGroundedContext(
                        1L,
                        0,
                        180
                );

        System.out.println(
                "========== AI CONTEXT =========="
        );

        System.out.println(context);

        System.out.println(
                "================================"
        );

        assertFalse(
                context.isBlank()
        );
    }
    @Test
    void shouldGenerateAnswerUsingGemini() {

        String answer =
                aiContentEngine.answerQuestion(
                        "Who appeared at 120 seconds?",
                        1L,
                        0,
                        180
                );

        System.out.println(
                "========== GEMINI ANSWER =========="
        );

        System.out.println(answer);

        System.out.println(
                "===================================="
        );

        assertFalse(answer.isBlank());
    }
    @Test
    void shouldGenerateTriviaUsingTimeline() {

        String trivia =
                aiContentEngine.generateTrivia(
                        1L,
                        60,
                        180
                );

        System.out.println("========== GEMINI TRIVIA ==========");
        System.out.println(trivia);
        System.out.println("===================================");

        assertFalse(trivia.isBlank());
    }
}
