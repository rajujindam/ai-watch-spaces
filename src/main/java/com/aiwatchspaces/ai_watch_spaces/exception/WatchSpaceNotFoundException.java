
package com.aiwatchspaces.ai_watch_spaces.exception;

public class WatchSpaceNotFoundException extends RuntimeException {

    public WatchSpaceNotFoundException(Long id) {
        super("Watch Space not found with id: " + id);
    }
    public WatchSpaceNotFoundException(String message){
        super(message);
    }
}