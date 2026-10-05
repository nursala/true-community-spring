package com.truecommunity.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * REST-only exception advice that converts expected domain failures into the
 * shared {@link ErrorResponse} JSON shape. Highest precedence keeps REST
 * responses JSON even though REST controllers are also MVC controllers.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(StatementNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStatementNotFound(StatementNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(VoteAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleVoteAlreadyExists(VoteAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(VotingClosedException.class)
    public ResponseEntity<ErrorResponse> handleVotingClosed(VotingClosedException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EditorDecisionAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEditorDecisionAlreadyExists(EditorDecisionAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidStatementStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatementState(InvalidStatementStateException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(StatementAlreadyClaimedException.class)
    public ResponseEntity<ErrorResponse> handleStatementAlreadyClaimed(StatementAlreadyClaimedException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(SpeakerInUseException.class)
    public ResponseEntity<ErrorResponse> handleSpeakerInUse(SpeakerInUseException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({
            CannotDemoteSelfException.class,
            CannotDemoteProtectedUserException.class,
            CannotDemoteLastEditorException.class
    })
    public ResponseEntity<ErrorResponse> handleDemotionConflict(RuntimeException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsernameAlreadyExists(UsernameAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMismatch(PasswordMismatchException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidTopicSelectionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTopicSelection(InvalidTopicSelectionException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidUrlException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUrl(InvalidUrlException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedEditorActionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedEditorAction(UnauthorizedEditorActionException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedDiscussionAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedDiscussionAccess(UnauthorizedDiscussionAccessException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ConsensusAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleConsensusAccessDenied(ConsensusAccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed.");

        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred.");
    }

    /**
     * Builds the common REST error response body for a status category.
     *
     * @param status HTTP status to return
     * @param message error message to include in the payload
     * @return response entity containing {@link ErrorResponse}
     */
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        ErrorResponse response = new ErrorResponse(
                status.getReasonPhrase(),
                message,
                status.value(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(status).body(response);
    }
}
