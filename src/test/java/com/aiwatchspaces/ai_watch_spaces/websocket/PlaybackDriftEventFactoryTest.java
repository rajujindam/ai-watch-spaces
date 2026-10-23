
package com.aiwatchspaces.ai_watch_spaces.websocket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlaybackDriftEventFactoryTest {

    private final PlaybackDriftEventFactory factory =
            new PlaybackDriftEventFactory();

    @Test
    void shouldReturnNoneWhenDriftIsWithin100Milliseconds() {
        WebSocketEvent event =
                factory.createDriftReport(10.0, 10.1, 0.1);

        assertEquals("playback.drift", event.getType());
        assertEquals("NONE", event.getData().get("correction"));
    }

    @Test
    void shouldReturnAdjustWhenDriftIsBetween100And250Milliseconds() {
        WebSocketEvent event =
                factory.createDriftReport(10.0, 10.2, 0.2);

        assertEquals("ADJUST", event.getData().get("correction"));
    }

    @Test
    void shouldReturnSeekWhenDriftExceeds250Milliseconds() {
        WebSocketEvent event =
                factory.createDriftReport(10.0, 10.3, 0.3);

        assertEquals("SEEK", event.getData().get("correction"));
    }

    @Test
    void shouldHandleNegativeDriftUsingItsAbsoluteValue() {
        WebSocketEvent event =
                factory.createDriftReport(10.3, 10.0, -0.3);

        assertEquals("SEEK", event.getData().get("correction"));
    }

    @Test
    void shouldReturnAdjustAtExactly250Milliseconds() {
        WebSocketEvent event =
                factory.createDriftReport(10.0, 10.25, 0.25);

        assertEquals("ADJUST", event.getData().get("correction"));
    }

    @Test
    void shouldReturnSeekWhenDriftIsJustAbove250Milliseconds() {
        WebSocketEvent event =
                factory.createDriftReport(10.0, 10.251, 0.251);

        assertEquals("SEEK", event.getData().get("correction"));
    }

    @Test
    void shouldIncludeExpectedPositionAsTargetPosition() {
        WebSocketEvent event =
                factory.createDriftReport(10.0, 10.2, 0.2);

        assertEquals(
                10.2,
                (Double) event.getData().get("targetPositionSeconds"),
                0.000001
        );
    }

    @Test
    void shouldReturnNoneAtExactly100Milliseconds() {
        WebSocketEvent event =
                factory.createDriftReport(10.0, 10.1, 0.1);

        assertEquals("NONE", event.getData().get("correction"));
    }

}
