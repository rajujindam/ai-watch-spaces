package com.aiwatchspaces.ai_watch_spaces.exception;

public class WatchSpaceParticipantNotFoundException
        extends RuntimeException {

    public WatchSpaceParticipantNotFoundException(
            Long watchSpaceId,
            Long participantUserId) {

        super("Participant " + participantUserId
                + " was not found in watch space " + watchSpaceId);
    }
}