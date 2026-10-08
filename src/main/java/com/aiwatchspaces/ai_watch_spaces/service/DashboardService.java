package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.dto.DashboardResponse;
import com.aiwatchspaces.ai_watch_spaces.dto.RecommendationResponse;
import com.aiwatchspaces.ai_watch_spaces.dto.TitleResponse;
import com.aiwatchspaces.ai_watch_spaces.dto.WatchSpaceResponse;
import com.aiwatchspaces.ai_watch_spaces.entity.*;
import com.aiwatchspaces.ai_watch_spaces.repository.InteractionRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.TitleRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.WatchSpaceParticipantRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.WatchSpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InteractionRepository interactionRepository;
    private final TitleRepository titleRepository;
    private final WatchSpaceRepository watchSpaceRepository;
    private final WatchSpaceParticipantRepository participantRepository;
    private final RecommendationService recommendationService;

    @Transactional(readOnly = true)
    public List<TitleResponse> getRecentlyWatched(Long userId) {

        List<Interaction> interactions =
                interactionRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return interactions.stream()
                .map(interaction -> titleRepository.findById(interaction.getTitleId())
                        .orElse(null))
                .filter(title -> title != null)
                .map(this::toTitleResponse)
                .toList();
    }

    private TitleResponse toTitleResponse(Title title) {
        return new TitleResponse(
                title.getId(),
                title.getName(),
                title.getDescription(),
                title.getDurationSeconds()
        );
    }
    @Transactional(readOnly = true)
    public List<WatchSpaceResponse> getUpcomingWatchSpaces(Long userId) {

        return watchSpaceRepository
                .findByHostIdAndStatus(
                        userId,
                        WatchSpaceStatus.SCHEDULED
                )
                .stream()
                .map(this::toWatchSpaceResponse)
                .toList();
    }
    private WatchSpaceResponse toWatchSpaceResponse(WatchSpace watchSpace) {
        return new WatchSpaceResponse(
                watchSpace.getId(),
                watchSpace.getName(),
                watchSpace.getHostId(),
                watchSpace.getTitleId(),
                watchSpace.getStatus(),
                watchSpace.getMaxParticipants(),
                watchSpace.getAiVerbosity(),
                watchSpace.getVotingEnabled(),
                watchSpace.getCreatedAt(),
                watchSpace.getEndedAt()
        );
    }
    @Transactional(readOnly = true)
    public List<WatchSpaceResponse> getQuickRejoinWatchSpaces(Long userId) {

        List<WatchSpace> liveRooms = new ArrayList<>();

        // Live rooms hosted by the user
        liveRooms.addAll(
                watchSpaceRepository.findByHostIdAndStatus(
                        userId,
                        WatchSpaceStatus.LIVE
                )
        );

        // Live rooms where the user is a participant
        List<WatchSpaceParticipant> participations =
                participantRepository.findByUserId(userId);

        for (WatchSpaceParticipant participation : participations) {

            WatchSpace room = participation.getWatchSpace();

            if (room.getStatus() == WatchSpaceStatus.LIVE) {
                liveRooms.add(room);
            }
        }

        return liveRooms.stream()
                .distinct()
                .map(this::toWatchSpaceResponse)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(Long userId) {

        return recommendationService
                .recommend(userId, 5)
                .stream()
                .map(recommendation -> new RecommendationResponse(
                        recommendation.title().getId(),
                        recommendation.title().getName(),
                        recommendation.contentScore(),
                        recommendation.collaborativeScore(),
                        recommendation.hybridScore()
                ))
                .toList();
    }
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId) {

        return new DashboardResponse(
                getRecentlyWatched(userId),
                getUpcomingWatchSpaces(userId),
                getQuickRejoinWatchSpaces(userId),
                getRecommendations(userId)
        );
    }
}