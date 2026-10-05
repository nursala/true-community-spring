package com.truecommunity.exception;

/**
 * 409 conflict raised when an editor claim loses to an existing claim.
 */
public class StatementAlreadyClaimedException extends RuntimeException {

    public StatementAlreadyClaimedException(String message) {
        super(message);
    }
}
