package com.truecommunity.exception;

/**
 * 404 missing-resource exception for statement lookup failures.
 */
public class StatementNotFoundException extends RuntimeException {

    public StatementNotFoundException(String message) {
        super(message);
    }
}
