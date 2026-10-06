package com.aiwatchspaces.ai_watch_spaces.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("gemini")
public class GeminiProvider implements AiProvider {

    private final Client client;

    public GeminiProvider() {
        this.client = new Client();
    }

    @Override
    public String generateAnswer(String question, String context) {

        String prompt = """
                You are the AI assistant for a shared watch space.

                Answer the user's question using only the supplied timeline context.

                If the context does not contain enough information to answer the question,
                say that the information is not available in the supplied timeline.

                Do not invent events, characters, motivations, or facts.

                Timeline context:
                %s

                User question:
                %s
                """.formatted(context, question);

        GenerateContentResponse response =
                client.models.generateContent(
                        "gemini-3.8-flash",
                        prompt,
                        null
                );

        return response.text();
    }
    @Override
    public String generateTrivia(String context) {

        String prompt = """
            You are the AI trivia generator for a shared watch space.

            Your task is to create exactly ONE simple trivia question
            from the supplied timeline context.

            IMPORTANT RULES:
            1. Use ONLY information explicitly present in the timeline context.
            2. The timeline event itself is sufficient to create trivia.
            3. Do not use outside knowledge.
            4. Do not invent events, characters, locations, motivations, or facts.
            5. The answer must be directly supported by the timeline.
            6. Prefer a simple factual question about the event.

            For example, if the timeline says:

            At 120 seconds:
            Event type: CHARACTER_APPEARANCE
            Details: {character=Example Character, description=First appearance}

            A valid trivia question would be:

            Question: Which character appeared at 120 seconds?
            Answer: Example Character

            Return the result in exactly this format:

            Question: <question>
            Answer: <answer>

            Timeline context:
            %s
            """.formatted(context);

        GenerateContentResponse response =
                client.models.generateContent(
                        "gemini-3.8-flash",
                        prompt,
                        null
                );

        return response.text();
    }
}