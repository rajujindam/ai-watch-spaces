package com.aiwatchspaces.ai_watch_spaces.exception;

public class WatchSpaceCapacityExceededException
        extends RuntimeException {

    public WatchSpaceCapacityExceededException(Long watchSpaceId) {
        super("Watch space " + watchSpaceId
                + " has reached its maximum participant capacity");
    }
}