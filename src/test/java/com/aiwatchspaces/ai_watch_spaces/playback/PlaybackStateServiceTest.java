
package com.aiwatchspaces.ai_watch_spaces.playback;

import com.aiwatchspaces.ai_watch_spaces.entity.Title;
import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpace;
import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpaceStatus;
import com.aiwatchspaces.ai_watch_spaces.repository.TitleRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.WatchSpaceParticipantRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.WatchSpaceRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlaybackStateServiceTest {

    private WatchSpaceRepository watchSpaceRepository;
    private WatchSpaceParticipantRepository participantRepository;
    private TitleRepository titleRepository;
    private PlaybackStateService playbackStateService;

    @BeforeEach
    void setUp() {
        watchSpaceRepository = mock(WatchSpaceRepository.class);
        participantRepository =
                mock(WatchSpaceParticipantRepository.class);
        titleRepository = mock(TitleRepository.class);

        playbackStateService = new PlaybackStateService(
                watchSpaceRepository,
                participantRepository,
                titleRepository
        );
    }

    @Test
    void shouldPausePlaybackAtCurrentPositionWhenHostDisconnects() {
        Long watchSpaceId = 4L;
        Long hostId = 2L;
        Long titleId = 10L;

        WatchSpace room = new WatchSpace(
                "Test Room",
                hostId,
                titleId,
                50,
                null,
                false
        );

        room.setId(watchSpaceId);
        room.setStatus(WatchSpaceStatus.LIVE);

        Title title = new Title(
                "Test Movie",
                "Test description",
                3600
        );
        title.setId(titleId);

        when(watchSpaceRepository.findById(watchSpaceId))
                .thenReturn(Optional.of(room));

        when(titleRepository.findById(titleId))
                .thenReturn(Optional.of(title));

        // The service's internal state starts paused at zero.
        // Start playback through the normal host-controlled operation.
        playbackStateService.play(watchSpaceId, hostId);

        PlaybackState pausedState =
                playbackStateService.pauseOnHostDisconnect(watchSpaceId);

        assertEquals(PlaybackStatus.PAUSED, pausedState.getStatus());
        assertTrue(pausedState.getPositionSeconds() >= 0.0);
        assertTrue(pausedState.getPositionSeconds() <= 3600.0);
    }

    @Test
    void shouldPreservePositionWhenPlaybackIsPaused() {
        Long watchSpaceId = 4L;
        Long hostId = 2L;
        Long titleId = 10L;

        WatchSpace room = new WatchSpace(
                "Test Room", hostId, titleId, 50, null, false
        );
        room.setId(watchSpaceId);
        room.setStatus(WatchSpaceStatus.LIVE);

        Title title = new Title("Test Movie", "Description", 3600);

        when(watchSpaceRepository.findById(watchSpaceId))
                .thenReturn(Optional.of(room));
        when(titleRepository.findById(titleId))
                .thenReturn(Optional.of(title));

        playbackStateService.play(watchSpaceId, hostId);

        PlaybackState paused =
                playbackStateService.pause(watchSpaceId, hostId);

        double pausedPosition = paused.getPositionSeconds();

        PlaybackState current =
                playbackStateService.getState(watchSpaceId, hostId);

        assertEquals(PlaybackStatus.PAUSED, current.getStatus());
        assertEquals(
                pausedPosition,
                current.getPositionSeconds(),
                0.001
        );
    }

    @Test
    void shouldContinuePlayingAfterSeek() {
        Long watchSpaceId = 4L;
        Long hostId = 2L;
        Long titleId = 10L;
        double targetPosition = 120.0;

        WatchSpace room = new WatchSpace(
                "Test Room", hostId, titleId, 50, null, false
        );
        room.setId(watchSpaceId);
        room.setStatus(WatchSpaceStatus.LIVE);

        Title title = new Title("Test Movie", "Description", 3600);

        when(watchSpaceRepository.findById(watchSpaceId))
                .thenReturn(Optional.of(room));
        when(titleRepository.findById(titleId))
                .thenReturn(Optional.of(title));

        playbackStateService.play(watchSpaceId, hostId);

        PlaybackState seeked =
                playbackStateService.seek(
                        watchSpaceId, hostId, targetPosition
                );

        assertEquals(PlaybackStatus.PLAYING, seeked.getStatus());
        assertEquals(
                targetPosition,
                seeked.getPositionSeconds(),
                0.001
        );
    }

    @Test
    void shouldRejectNegativeSeekPosition() {
        Long watchSpaceId = 4L;
        Long hostId = 2L;
        Long titleId = 10L;

        WatchSpace room = new WatchSpace(
                "Test Room", hostId, titleId, 50, null, false
        );
        room.setId(watchSpaceId);

        Title title = new Title("Test Movie", "Description", 3600);

        when(watchSpaceRepository.findById(watchSpaceId))
                .thenReturn(Optional.of(room));
        when(titleRepository.findById(titleId))
                .thenReturn(Optional.of(title));

        assertThrows(
                IllegalArgumentException.class,
                () -> playbackStateService.seek(watchSpaceId, hostId, -1.0)
        );
    }

    @Test
    void shouldRejectSeekBeyondTitleDuration() {
        Long watchSpaceId = 4L;
        Long hostId = 2L;
        Long titleId = 10L;

        WatchSpace room = new WatchSpace(
                "Test Room", hostId, titleId, 50, null, false
        );
        room.setId(watchSpaceId);

        Title title = new Title("Test Movie", "Description", 3600);

        when(watchSpaceRepository.findById(watchSpaceId))
                .thenReturn(Optional.of(room));
        when(titleRepository.findById(titleId))
                .thenReturn(Optional.of(title));

        assertThrows(
                IllegalArgumentException.class,
                () -> playbackStateService.seek(watchSpaceId, hostId, 3601.0)
        );
    }
    @Test
    void shouldPausePlaybackWhenTitleDurationIsReached() {
        WatchSpace watchSpace = new WatchSpace();
        watchSpace.setId(4L);
        watchSpace.setHostId(2L);
        watchSpace.setTitleId(10L);

        Title title = new Title("Test Movie", "Description", 1);

        when(watchSpaceRepository.findById(4L))
                .thenReturn(Optional.of(watchSpace));
        when(titleRepository.findById(10L))
                .thenReturn(Optional.of(title));

        playbackStateService.play(4L, 2L);

        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        PlaybackState state = playbackStateService.getState(4L, 2L);

        assertEquals(PlaybackStatus.PAUSED, state.getStatus());
        assertEquals(1.0, state.getPositionSeconds(), 0.001);
    }
    @Test
    void shouldRemainPausedAtDurationAfterPlaybackEnds() {
        WatchSpace watchSpace = new WatchSpace();
        watchSpace.setId(4L);
        watchSpace.setHostId(2L);
        watchSpace.setTitleId(10L);

        Title title = new Title("Test Movie", "Description", 1);

        when(watchSpaceRepository.findById(4L))
                .thenReturn(Optional.of(watchSpace));
        when(titleRepository.findById(10L))
                .thenReturn(Optional.of(title));

        playbackStateService.play(4L, 2L);

        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        PlaybackState firstState =
                playbackStateService.getState(4L, 2L);

        PlaybackState secondState =
                playbackStateService.getState(4L, 2L);

        assertEquals(PlaybackStatus.PAUSED, firstState.getStatus());
        assertEquals(PlaybackStatus.PAUSED, secondState.getStatus());
        assertEquals(1.0, secondState.getPositionSeconds(), 0.001);
    }
}
