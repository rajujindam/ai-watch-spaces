
package com.aiwatchspaces.ai_watch_spaces.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WatchSpaceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleWatchSpaceNotFound(
            WatchSpaceNotFoundException exception) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(ParticipantAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateParticipant(
            ParticipantAlreadyExistsException exception) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(InvalidInviteCodeException.class)
    public ResponseEntity<Map<String, String>> handleInvalidInviteCode(
            InvalidInviteCodeException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(org.springframework.core.io.buffer.DataBufferLimitException.class)
    public ResponseEntity<Map<String, String>> handleUploadTooLarge(
            org.springframework.core.io.buffer.DataBufferLimitException exception) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", "Uploaded file exceeds the 1 MiB limit"));
    }

    @ExceptionHandler(TitleNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTitleNotFound(
            TitleNotFoundException exception) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(InvalidTimelineRequestException.class)
    public ResponseEntity<Map<String, String>> handleInvalidTimelineRequest(
            InvalidTimelineRequestException ex
    ) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUserAlreadyExists(
            UserAlreadyExistsException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(
            InvalidCredentialsException ex
    ) {
        Map<String, String> error = new HashMap<>();

        error.put("error", "Unauthorized");
        error.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(error);
    }

    @ExceptionHandler(WatchSpaceAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleWatchSpaceAccessDenied(
            WatchSpaceAccessDeniedException ex) {

        Map<String, String> error = new HashMap<>();
        error.put("error", "Forbidden");
        error.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error);
    }

    @ExceptionHandler(TimelineEventNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTimelineEventNotFound(
            TimelineEventNotFoundException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(WatchSpaceCapacityExceededException.class)
    public ResponseEntity<ProblemDetail> handleCapacityExceeded(
            WatchSpaceCapacityExceededException ex) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problem);
    }
    @ExceptionHandler(WatchSpaceLockedException.class)
    public ResponseEntity<ProblemDetail> handleWatchSpaceLocked(
            WatchSpaceLockedException ex) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problem);
    }
    @ExceptionHandler(WatchSpaceParticipantNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleParticipantNotFound(
            WatchSpaceParticipantNotFoundException ex) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problem);
    }
}