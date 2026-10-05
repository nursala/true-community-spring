package com.truecommunity.exception;

/**
 * 409 conflict raised when a final editor decision already exists for a statement.
 */
public class EditorDecisionAlreadyExistsException extends RuntimeException {

    public EditorDecisionAlreadyExistsException(String message) {
        super(message);
    }
}
