package com.truecommunity.exception;

/**
 * 409 conflict raised when deleting a statement that already has votes, an
 * editor decision, or is claimed/under review.
 */
public class StatementInUseException extends RuntimeException {

    public StatementInUseException(String message) {
        super(message);
    }
}
