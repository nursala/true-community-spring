package com.truecommunity.exception;

/**
 * 403 authorization exception for anti-bias consensus access violations.
 */
public class ConsensusAccessDeniedException extends RuntimeException {

    public ConsensusAccessDeniedException(String message) {
        super(message);
    }
}
