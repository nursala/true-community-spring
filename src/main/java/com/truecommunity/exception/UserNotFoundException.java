package com.truecommunity.exception;

/**
 * 404 missing-resource exception for user lookup failures.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
