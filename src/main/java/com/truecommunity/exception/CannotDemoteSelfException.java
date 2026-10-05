package com.truecommunity.exception;

/**
 * 409 conflict raised when an editor attempts to demote their own account.
 */
public class CannotDemoteSelfException extends RuntimeException {

    public CannotDemoteSelfException(String message) {
        super(message);
    }
}
