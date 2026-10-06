package com.aiwatchspaces.ai_watch_spaces.ai;

public interface AiProvider {

    String generateAnswer(
            String question,
            String context
    );
    String generateTrivia(String context);
}