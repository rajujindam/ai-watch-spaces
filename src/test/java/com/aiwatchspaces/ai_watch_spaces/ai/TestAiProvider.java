package com.aiwatchspaces.ai_watch_spaces.ai;

public class TestAiProvider implements AiProvider {

    @Override
    public String generateAnswer(String question, String context) {
        return "Test answer";
    }

    @Override
    public String generateTrivia(String context) {
        return """
                Question: Which character appeared at 120 seconds?
                Answer: Example Character
                """;
    }
}