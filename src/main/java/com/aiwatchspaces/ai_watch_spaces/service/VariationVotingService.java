package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.dto.VariationOptionResponse;
import com.aiwatchspaces.ai_watch_spaces.dto.VariationVoteResponse;
import com.aiwatchspaces.ai_watch_spaces.dto.VariationVoteResult;
import com.aiwatchspaces.ai_watch_spaces.entity.TimelineEvent;
import com.aiwatchspaces.ai_watch_spaces.entity.VariationOption;
import com.aiwatchspaces.ai_watch_spaces.exception.TimelineEventNotFoundException;
import com.aiwatchspaces.ai_watch_spaces.repository.TimelineEventRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.VariationOptionRepository;
import com.aiwatchspaces.ai_watch_spaces.websocket.ActiveVote;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class VariationVotingService {

    private final TimelineEventRepository timelineEventRepository;
    private final VariationOptionRepository variationOptionRepository;
    private final WatchSpaceService watchSpaceService;

    private final ConcurrentMap<Long, ActiveVote> activeVotes =
            new ConcurrentHashMap<>();

    public VariationVoteResponse openVote(
            Long watchSpaceId,
            Long userId,
            Long variationId,
            Integer durationSeconds
    ) {
        watchSpaceService.checkHost(watchSpaceId, userId);

        TimelineEvent timelineEvent =
                timelineEventRepository.findById(variationId)
                        .orElseThrow(() ->
                                new TimelineEventNotFoundException(
                                        "Timeline event not found with id: "
                                                + variationId
                                )
                        );

        if (!"variation_point".equals(timelineEvent.getEventType())) {
            throw new IllegalArgumentException(
                    "Timeline event is not a variation point"
            );
        }

        List<VariationOption> options =
                variationOptionRepository
                        .findByTimelineEvent_Id(variationId);

        if (options.size() < 2) {
            throw new IllegalArgumentException(
                    "A variation point must have at least two options"
            );
        }

        if (activeVotes.containsKey(watchSpaceId)) {
            throw new IllegalStateException(
                    "A vote is already active in this watch space"
            );
        }

        Instant closesAt =
                Instant.now().plus(
                        Duration.ofSeconds(durationSeconds)
                );

        ActiveVote activeVote =
                new ActiveVote(
                        watchSpaceId,
                        variationId,
                        closesAt
                );

        activeVotes.put(watchSpaceId, activeVote);

        List<VariationOptionResponse> optionResponses =
                options.stream()
                        .map(option -> new VariationOptionResponse(
                                option.getId(),
                                option.getTimelineEvent().getId(),
                                option.getLabel(),
                                option.getAssetRef(),
                                option.getVoteCount()
                        ))
                        .toList();

        return new VariationVoteResponse(
                variationId,
                optionResponses,
                closesAt
        );
    }
    public void castVote(
            Long watchSpaceId,
            Long userId,
            Long variationId,
            Long optionId
    ) {
        ActiveVote activeVote = activeVotes.get(watchSpaceId);

        if (activeVote == null) {
            throw new IllegalStateException(
                    "No active vote exists in this watch space"
            );
        }

        if (!activeVote.getVariationId().equals(variationId)) {
            throw new IllegalArgumentException(
                    "Variation does not match the active vote"
            );
        }

        if (Instant.now().isAfter(activeVote.getClosesAt())) {
            throw new IllegalStateException(
                    "Voting period has ended"
            );
        }

        if (activeVote.hasUserVoted(userId)) {
            throw new IllegalStateException(
                    "User has already voted"
            );
        }

        VariationOption option =
                variationOptionRepository.findById(optionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Variation option not found"
                                )
                        );

        if (!option.getTimelineEvent().getId().equals(variationId)) {
            throw new IllegalArgumentException(
                    "Option does not belong to this variation"
            );
        }

        activeVote.addVote(userId, optionId);
    }
    @Transactional
    public VariationVoteResult closeVote(
            Long watchSpaceId,
            Long userId
    ) {
        watchSpaceService.checkHost(
                watchSpaceId,
                userId
        );

        ActiveVote activeVote = activeVotes.get(watchSpaceId);

        if (activeVote == null) {
            throw new IllegalStateException(
                    "No active vote exists in this watch space"
            );
        }

        Map<Long, Long> voteCounts = new HashMap<>();

        for (Long optionId : activeVote.getVotes().values()) {
            voteCounts.merge(optionId, 1L, Long::sum);
        }

        if (voteCounts.isEmpty()) {
            activeVotes.remove(watchSpaceId);

            throw new IllegalStateException(
                    "No votes were cast"
            );
        }

        for (Map.Entry<Long, Long> entry : voteCounts.entrySet()) {

            VariationOption option =
                    variationOptionRepository.findById(entry.getKey())
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Variation option not found"
                                    )
                            );

            option.setVoteCount(
                    option.getVoteCount()
                            + entry.getValue().intValue()
            );

            variationOptionRepository.save(option);
        }

        Long winningOptionId = voteCounts.entrySet()
                .stream()
                .max(
                        Comparator
                                .comparingLong(
                                        Map.Entry<Long, Long>::getValue
                                )
                                .thenComparing(
                                        Map.Entry<Long, Long>::getKey,
                                        Comparator.reverseOrder()
                                )
                )
                .map(Map.Entry::getKey)
                .orElseThrow();

        VariationOption winningOption =
                variationOptionRepository.findById(winningOptionId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Winning variation option not found"
                                )
                        );

        TimelineEvent timelineEvent =
                timelineEventRepository.findById(activeVote.getVariationId())
                        .orElseThrow(() ->
                                new TimelineEventNotFoundException(
                                        "Timeline event not found with id: "
                                                + activeVote.getVariationId()
                                )
                        );

        activeVotes.remove(watchSpaceId);

        return new VariationVoteResult(
                activeVote.getVariationId(),
                winningOptionId,
                timelineEvent.getTimestampSeconds(),
                winningOption.getAssetRef()
        );
    }
}