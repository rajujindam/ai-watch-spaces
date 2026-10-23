
package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.dto.CreateWatchSpaceRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.ParticipantResponse;
import com.aiwatchspaces.ai_watch_spaces.dto.WatchSpaceResponse;

import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpace;
import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpaceParticipant;

import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpaceStatus;
import com.aiwatchspaces.ai_watch_spaces.exception.*;

import com.aiwatchspaces.ai_watch_spaces.repository.TitleRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.WatchSpaceParticipantRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.WatchSpaceRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

        if (!titleRepository.existsById(request.getTitleId())) {
            throw new TitleNotFoundException("Title Not available");
        }

        WatchSpace watchSpace = new WatchSpace(
                request.getName(),
                userId,
                request.getTitleId(),
                request.getMaxParticipants(),
                request.getAiVerbosity(),
                request.getVotingEnabled()
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

        WatchSpace watchSpace = watchSpaceRepository
                .findByIdForUpdate(watchSpaceId)
                .orElseThrow(() ->
                        new WatchSpaceNotFoundException(watchSpaceId)
                );

        if (Boolean.TRUE.equals(watchSpace.getLocked())) {
            throw new WatchSpaceLockedException(watchSpaceId);
        }

        if (participantRepository
                .existsByWatchSpace_IdAndUserId(watchSpaceId, userId)) {

            throw new ParticipantAlreadyExistsException(
                    watchSpaceId,
                    userId
            );
        }

        long participantCount =
                participantRepository.countByWatchSpace_Id(watchSpaceId);

        if (participantCount >= watchSpace.getMaxParticipants()) {
            throw new WatchSpaceCapacityExceededException(watchSpaceId);
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
                watchSpace.getStatus(),
                watchSpace.getMaxParticipants(),
                watchSpace.getAiVerbosity(),
                watchSpace.getVotingEnabled(),
                watchSpace.getCreatedAt(),
                watchSpace.getEndedAt()
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
    @Transactional
    public WatchSpaceResponse endWatchSpace(Long watchSpaceId, Long userId) {

        WatchSpace watchSpace = findWatchSpace(watchSpaceId);

        if (!watchSpace.getHostId().equals(userId)) {
            throw new WatchSpaceAccessDeniedException(watchSpaceId);
        }

        if (watchSpace.getStatus() != WatchSpaceStatus.LIVE) {
            throw new IllegalStateException(
                    "Only a live watch space can be ended"
            );
        }

        watchSpace.setStatus(WatchSpaceStatus.ENDED);
        watchSpace.setEndedAt(LocalDateTime.now());

        WatchSpace savedWatchSpace =
                watchSpaceRepository.save(watchSpace);

        return toWatchSpaceResponse(savedWatchSpace);
    }

    @Transactional(readOnly = true)
    public Long getHostId(Long watchSpaceId) {
        return findWatchSpace(watchSpaceId).getHostId();
    }

    @Transactional
    public void setWatchSpaceLocked(
            Long watchSpaceId,
            Long userId,
            boolean locked) {

        WatchSpace watchSpace = findWatchSpace(watchSpaceId);

        if (!watchSpace.getHostId().equals(userId)) {
            throw new WatchSpaceAccessDeniedException(
                    "Only the host can lock or unlock this watch space"
            );
        }

        watchSpace.setLocked(locked);
    }
    @Transactional
    public void removeParticipant(
            Long watchSpaceId,
            Long hostId,
            Long participantUserId) {

        WatchSpace watchSpace = findWatchSpace(watchSpaceId);

        if (!watchSpace.getHostId().equals(hostId)) {
            throw new WatchSpaceAccessDeniedException(watchSpaceId);
        }

        if (watchSpace.getHostId().equals(participantUserId)) {
            throw new IllegalArgumentException(
                    "The host cannot remove themselves as a participant"
            );
        }

        WatchSpaceParticipant participant =
                participantRepository
                        .findByWatchSpace_IdAndUserId(
                                watchSpaceId,
                                participantUserId
                        )
                        .orElseThrow(() ->
                                new WatchSpaceParticipantNotFoundException(
                                        watchSpaceId,
                                        participantUserId
                                )
                        );

        participantRepository.delete(participant);
    }
    @Transactional
    public void transferHost(
            Long watchSpaceId,
            Long currentHostId,
            Long newHostId) {

        WatchSpace watchSpace = findWatchSpace(watchSpaceId);

        if (!watchSpace.getHostId().equals(currentHostId)) {
            throw new WatchSpaceAccessDeniedException(watchSpaceId);
        }

        if (currentHostId.equals(newHostId)) {
            throw new IllegalArgumentException(
                    "You are already the host of this watch space"
            );
        }

        boolean isParticipant =
                participantRepository
                        .existsByWatchSpace_IdAndUserId(
                                watchSpaceId,
                                newHostId
                        );

        if (!isParticipant) {
            throw new WatchSpaceParticipantNotFoundException(
                    watchSpaceId,
                    newHostId
            );
        }

        watchSpace.setHostId(newHostId);
    }
    @Transactional
    public String createInviteCode(Long watchSpaceId, Long userId) {
        WatchSpace watchSpace = findWatchSpace(watchSpaceId);

        if (!watchSpace.getHostId().equals(userId)) {
            throw new WatchSpaceAccessDeniedException(watchSpaceId);
        }

        if (watchSpace.getInviteCode() == null) {
            watchSpace.setInviteCode(UUID.randomUUID().toString());
        }

        return watchSpace.getInviteCode();
    }

    @Transactional
    public ParticipantResponse joinWatchSpaceByInvite(
            String inviteCode,
            Long userId
    ) {
        WatchSpace watchSpace = watchSpaceRepository
                .findByInviteCode(inviteCode)
                .orElseThrow(InvalidInviteCodeException::new);

        return joinWatchSpace(watchSpace.getId(), userId);
    }
}