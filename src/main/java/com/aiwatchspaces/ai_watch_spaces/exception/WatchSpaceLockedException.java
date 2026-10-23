package com.aiwatchspaces.ai_watch_spaces.exception;

public class WatchSpaceLockedException extends RuntimeException {

    public WatchSpaceLockedException(Long watchSpaceId) {
        super("Watch space " + watchSpaceId
                + " is locked and cannot accept new participants");
    }
}