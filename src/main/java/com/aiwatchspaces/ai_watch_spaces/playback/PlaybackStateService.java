package com.aiwatchspaces.ai_watch_spaces.playback;

import com.aiwatchspaces.ai_watch_spaces.entity.Title;
import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpace;
import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpaceStatus;
import com.aiwatchspaces.ai_watch_spaces.exception.TitleNotFoundException;
import com.aiwatchspaces.ai_watch_spaces.exception.WatchSpaceAccessDeniedException;
import com.aiwatchspaces.ai_watch_spaces.exception.WatchSpaceNotFoundException;
import com.aiwatchspaces.ai_watch_spaces.repository.TitleRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.WatchSpaceParticipantRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.WatchSpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class PlaybackStateService {
    private final WatchSpaceRepository watchSpaceRepository;
    private final WatchSpaceParticipantRepository watchSpaceParticipantRepository;
    private final TitleRepository titleRepository;

    private final ConcurrentMap<Long, PlaybackState> states =
            new ConcurrentHashMap<>();

    public PlaybackState play(Long watchSpaceId, Long userId) {

        WatchSpace room = findRoom(watchSpaceId);
        checkHost(room, userId);

        if (room.getStatus() == WatchSpaceStatus.SCHEDULED) {
            room.setStatus(WatchSpaceStatus.LIVE);
            watchSpaceRepository.save(room);
        }

        PlaybackState state = getOrCreateState(watchSpaceId);

        synchronized (state) {

            double currentPosition =
                    getCurrentPosition(
                            state,
                            getTitleDuration(watchSpaceId)
                    );

            state.setPositionSeconds(currentPosition);
            state.setStatus(PlaybackStatus.PLAYING);
            state.setLastUpdatedAt(Instant.now());
            state.setUpdatedBy(userId);

            return copyState(state);
        }
    }

    public PlaybackState pause(Long watchSpaceId, Long userId) {

        WatchSpace room = findRoom(watchSpaceId);
        checkHost(room, userId);

        PlaybackState state = getOrCreateState(watchSpaceId);

        synchronized (state) {

            double currentPosition = getCurrentPosition(state,getTitleDuration(watchSpaceId));

            state.setPositionSeconds(currentPosition);
            state.setStatus(PlaybackStatus.PAUSED);
            state.setLastUpdatedAt(Instant.now());
            state.setUpdatedBy(userId);

            return copyState(state);
        }
    }

    public PlaybackState seek(Long watchSpaceId,Long userId, double positionSeconds) {

        WatchSpace room = findRoom(watchSpaceId);
        checkHost(room, userId);
        Title title = titleRepository.findById(room.getTitleId())
                .orElseThrow(() ->
                        new TitleNotFoundException("WatchSpace is without any video"));
        if (!Double.isFinite(positionSeconds)
                || positionSeconds < 0
                || positionSeconds > title.getDurationSeconds()) {

            throw new IllegalArgumentException(
                    "Invalid playback position"
            );
        }

        PlaybackState state = getOrCreateState(watchSpaceId);

        synchronized (state) {

            state.setPositionSeconds(positionSeconds);
            state.setLastUpdatedAt(Instant.now());
            state.setUpdatedBy(userId);

            return copyState(state);
        }
    }

    public PlaybackState getState(Long watchSpaceId, Long userId) {

        WatchSpace room = findRoom(watchSpaceId);
        checkMemberOrHost(room, userId);

        PlaybackState state = getOrCreateState(watchSpaceId);

        synchronized (state) {

            double currentPosition = getCurrentPosition(state,getTitleDuration(watchSpaceId));
            PlaybackState snapshot = copyState(state);
            snapshot.setPositionSeconds(currentPosition);

            return snapshot;
        }
    }

    private double getCurrentPosition(PlaybackState state, double durationSeconds) {
        if (state.getStatus() == PlaybackStatus.PAUSED) {
            return state.getPositionSeconds();
        }

        double elapsedSeconds =
                Duration.between(state.getLastUpdatedAt(), Instant.now())
                        .toNanos() / 1_000_000_000.0;

        double currentPosition =
                state.getPositionSeconds() + elapsedSeconds;

        if (currentPosition >= durationSeconds) {
            state.setPositionSeconds(durationSeconds);
            state.setStatus(PlaybackStatus.PAUSED);
            state.setLastUpdatedAt(Instant.now());

            return durationSeconds;
        }

        return currentPosition;
    }

    private PlaybackState getOrCreateState(Long watchSpaceId) {

        return states.computeIfAbsent(
                watchSpaceId,
                PlaybackState::new
        );
    }

    private WatchSpace findRoom(Long watchSpaceId) {

        return watchSpaceRepository.findById(watchSpaceId)
                .orElseThrow(() ->
                        new WatchSpaceNotFoundException(
                                "Watch space not found"
                        )
                );
    }

    private void checkHost(WatchSpace room, Long userId) {

        if (!room.getHostId().equals(userId)) {
            throw new WatchSpaceAccessDeniedException(
                    "Only the host can control playback"
            );
        }
    }

    private void checkMemberOrHost(
            WatchSpace room,
            Long userId
    ) {

        if (room.getHostId().equals(userId)) {
            return;
        }

        boolean isMember =
                watchSpaceParticipantRepository
                        .existsByWatchSpace_IdAndUserId(
                                room.getId(),
                                userId
                        );

        if (!isMember) {
            throw new WatchSpaceAccessDeniedException(
                    "You are not a member of this watch space"
            );
        }
    }

    private PlaybackState copyState(PlaybackState state) {

        PlaybackState copy =
                new PlaybackState(state.getWatchSpaceId());

        copy.setStatus(state.getStatus());
        copy.setPositionSeconds(state.getPositionSeconds());
        copy.setLastUpdatedAt(state.getLastUpdatedAt());
        copy.setUpdatedBy(state.getUpdatedBy());

        return copy;
    }
    private double getTitleDuration(Long watchSpaceId) {
        WatchSpace room = findRoom(watchSpaceId);

        Title title = titleRepository.findById(room.getTitleId())
                .orElseThrow(() ->
                        new TitleNotFoundException("WatchSpace is without any video"));

        return title.getDurationSeconds();
    }


    public PlaybackState pauseOnHostDisconnect(Long watchSpaceId) {
        WatchSpace room = findRoom(watchSpaceId);

        if (room.getStatus() != WatchSpaceStatus.LIVE) {
            return getStateForInternalUse(watchSpaceId);
        }

        PlaybackState state = getOrCreateState(watchSpaceId);

        synchronized (state) {
            double currentPosition =
                    getCurrentPosition(
                            state,
                            getTitleDuration(watchSpaceId)
                    );

            state.setPositionSeconds(currentPosition);
            state.setStatus(PlaybackStatus.PAUSED);
            state.setLastUpdatedAt(Instant.now());

            return copyState(state);
        }
    }

    private PlaybackState getStateForInternalUse(Long watchSpaceId) {
        PlaybackState state = getOrCreateState(watchSpaceId);

        synchronized (state) {
            PlaybackState snapshot = copyState(state);
            snapshot.setPositionSeconds(
                    getCurrentPosition(
                            state,
                            getTitleDuration(watchSpaceId)
                    )
            );
            return snapshot;
        }
    }

    public double getExpectedPosition(Long watchSpaceId, Long userId) {
        return getState(watchSpaceId, userId).getPositionSeconds();
    }

    public boolean isValidReportedPosition(
            Long watchSpaceId,
            double positionSeconds
    ) {
        if (!Double.isFinite(positionSeconds) || positionSeconds < 0) {
            return false;
        }

        return positionSeconds <= getTitleDuration(watchSpaceId);
    }

}
