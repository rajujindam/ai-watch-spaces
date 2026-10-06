
package com.aiwatchspaces.ai_watch_spaces.exception;

public class ParticipantAlreadyExistsException
        extends RuntimeException {

    public ParticipantAlreadyExistsException(
            Long watchSpaceId,
            Long userId) {

        super("User " + userId
                + " is already in Watch Space " + watchSpaceId);
    }
}