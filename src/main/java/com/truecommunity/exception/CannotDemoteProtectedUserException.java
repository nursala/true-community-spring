package com.truecommunity.exception;

/**
 * 409 conflict raised when a protected seeded editor is targeted for demotion.
 */
public class CannotDemoteProtectedUserException extends RuntimeException {

    public CannotDemoteProtectedUserException(String message) {
        super(message);
    }
}
