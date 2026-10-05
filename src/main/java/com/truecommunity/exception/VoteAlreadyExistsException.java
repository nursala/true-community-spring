package com.truecommunity.exception;

/**
 * 409 conflict raised when a user submits a duplicate vote for one statement.
 */
public class VoteAlreadyExistsException extends RuntimeException {

    public VoteAlreadyExistsException(String message) {
        super(message);
    }
}
