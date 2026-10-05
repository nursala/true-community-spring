package com.truecommunity.exception;

/**
 * 409 conflict raised when a statement workflow action is requested in the wrong state.
 */
public class InvalidStatementStateException extends RuntimeException {

    public InvalidStatementStateException(String message) {
        super(message);
    }
}
