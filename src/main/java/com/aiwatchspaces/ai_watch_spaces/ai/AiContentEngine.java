package com.aiwatchspaces.ai_watch_spaces.ai;

import com.aiwatchspaces.ai_watch_spaces.entity.TimelineEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiContentEngine {

    private final TimelineRetrievalService timelineRetrievalService;
    private final TimelineContextBuilder timelineContextBuilder;
    private final AiProvider aiProvider;

    public String buildGroundedContext(
            Long titleId,
            Integer startSeconds,
            Integer endSeconds
    ) {

        List<TimelineEvent> events =
                timelineRetrievalService.getEvents(
                        titleId,
                        startSeconds,
                        endSeconds
                );

        return timelineContextBuilder.buildContext(events);
    }
    public String answerQuestion(
            String question,
            Long titleId,
            Integer startSeconds,
            Integer endSeconds
    ) {

        List<TimelineEvent> events =
                timelineRetrievalService.getEvents(
                        titleId,
                        startSeconds,
                        endSeconds
                );

        String context =
                timelineContextBuilder.buildContext(events);

        return aiProvider.generateAnswer(
                question,
                context
        );
    }
    public String generateTrivia(
            Long titleId,
            Integer startSeconds,
            Integer endSeconds
    ) {
        List<TimelineEvent> events =
                timelineRetrievalService.getEvents(
                        titleId,
                        startSeconds,
                        endSeconds
                );

        String context =
                timelineContextBuilder.buildContext(events);

        return aiProvider.generateTrivia(context);
    }


    public GroundedAnswer answerQuestionWithSources(
            String question,
            Long titleId,
            Integer startSeconds,
            Integer endSeconds
    ) {
        List<TimelineEvent> events =
                timelineRetrievalService.getEvents(
                        titleId,
                        startSeconds,
                        endSeconds
                );

        if (events.isEmpty()) {
            return new GroundedAnswer(
                    "I couldn't find relevant timeline information to answer that question.",
                    events
            );
        }

        String context =
                timelineContextBuilder.buildContext(events);

        String answer =
                aiProvider.generateAnswer(
                        question,
                        context
                );

        return new GroundedAnswer(answer, events);
    }


}