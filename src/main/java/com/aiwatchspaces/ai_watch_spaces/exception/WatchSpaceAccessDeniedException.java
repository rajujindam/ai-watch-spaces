package com.aiwatchspaces.ai_watch_spaces.exception;

public class WatchSpaceAccessDeniedException extends RuntimeException {

    public WatchSpaceAccessDeniedException(Long watchSpaceId) {
        super("You do not have access to watch space: " + watchSpaceId);
    }
    public WatchSpaceAccessDeniedException(String message) {
        super(message);
    }
}