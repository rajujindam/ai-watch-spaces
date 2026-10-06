package com.aiwatchspaces.ai_watch_spaces.websocket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor
public class ActiveVote {

    private final Long watchSpaceId;
    private final Long variationId;
    private final Instant closesAt;

    private final Map<Long, Long> votes = new ConcurrentHashMap<>();
    public boolean hasUserVoted(Long userId) {
        return votes.containsKey(userId);
    }

    public void addVote(Long userId, Long optionId) {
        votes.put(userId, optionId);
    }
}