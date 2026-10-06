
package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.dto.CreateWatchSpaceRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.ParticipantResponse;
import com.aiwatchspaces.ai_watch_spaces.dto.WatchSpaceResponse;

import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpace;
import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpaceParticipant;

import com.aiwatchspaces.ai_watch_spaces.exception.ParticipantAlreadyExistsException;
import com.aiwatchspaces.ai_watch_spaces.exception.TitleNotFoundException;
import com.aiwatchspaces.ai_watch_spaces.exception.WatchSpaceAccessDeniedException;
import com.aiwatchspaces.ai_watch_spaces.exception.WatchSpaceNotFoundException;

import com.aiwatchspaces.ai_watch_spaces.repository.TitleRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.WatchSpaceParticipantRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.WatchSpaceRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchSpaceService {

    private final WatchSpaceParticipantRepository participantRepository;
    private final WatchSpaceRepository watchSpaceRepository;
    private final TitleRepository titleRepository;

    @Transactional
    public WatchSpaceResponse createWatchSpace(
            CreateWatchSpaceRequest request,
            Long userId) {
        if(!titleRepository.existsById(request.getTitleId())) throw new TitleNotFoundException("Title Not available");
        WatchSpace watchSpace = new WatchSpace(
                request.getName(),
                userId,
                request.getTitleId()
        );

        WatchSpace savedWatchSpace =
                watchSpaceRepository.save(watchSpace);

        return toWatchSpaceResponse(savedWatchSpace);
    }

    @Transactional(readOnly = true)
    public WatchSpaceResponse getWatchSpaceById(
            Long watchSpaceId,
            Long userId) {

        WatchSpace watchSpace = findWatchSpace(watchSpaceId);

        checkMembership(watchSpace, userId);

        return toWatchSpaceResponse(watchSpace);
    }

    @Transactional
    public ParticipantResponse joinWatchSpace(
            Long watchSpaceId,
            Long userId) {

        WatchSpace watchSpace = findWatchSpace(watchSpaceId);

        if (participantRepository
                .existsByWatchSpace_IdAndUserId(watchSpaceId, userId)) {

            throw new ParticipantAlreadyExistsException(
                    watchSpaceId,
                    userId
            );
        }

        WatchSpaceParticipant participant =
                new WatchSpaceParticipant(watchSpace, userId);

        WatchSpaceParticipant saved =
                participantRepository.save(participant);

        return new ParticipantResponse(
                saved.getId(),
                watchSpaceId,
                saved.getUserId(),
                saved.getJoinedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> getParticipants(
            Long watchSpaceId,
            Long userId) {

        WatchSpace watchSpace = findWatchSpace(watchSpaceId);

        checkMembership(watchSpace, userId);

        return participantRepository
                .findByWatchSpace_Id(watchSpaceId)
                .stream()
                .map(participant -> new ParticipantResponse(
                        participant.getId(),
                        watchSpaceId,
                        participant.getUserId(),
                        participant.getJoinedAt()
                ))
                .toList();
    }

    private WatchSpace findWatchSpace(Long watchSpaceId) {
        return watchSpaceRepository.findById(watchSpaceId)
                .orElseThrow(() ->
                        new WatchSpaceNotFoundException(watchSpaceId));
    }

    private void checkMembership(
            WatchSpace watchSpace,
            Long userId) {

        boolean isHost =
                watchSpace.getHostId().equals(userId);

        boolean isParticipant =
                participantRepository
                        .existsByWatchSpace_IdAndUserId(
                                watchSpace.getId(),
                                userId
                        );

        if (!isHost && !isParticipant) {
            throw new WatchSpaceAccessDeniedException(
                    watchSpace.getId()
            );
        }
    }

    private WatchSpaceResponse toWatchSpaceResponse(
            WatchSpace watchSpace) {

        return new WatchSpaceResponse(
                watchSpace.getId(),
                watchSpace.getName(),
                watchSpace.getHostId(),
                watchSpace.getTitleId(),
                watchSpace.getCreatedAt()
        );
    }
    @Transactional(readOnly = true)
    public void checkHost(
            Long watchSpaceId,
            Long userId
    ) {
        WatchSpace watchSpace = findWatchSpace(watchSpaceId);

        if (!watchSpace.getHostId().equals(userId)) {
            throw new WatchSpaceAccessDeniedException(
                    watchSpaceId
            );
        }
    }
}