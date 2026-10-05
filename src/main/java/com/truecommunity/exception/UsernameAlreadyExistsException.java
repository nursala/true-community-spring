package com.truecommunity.exception;

/**
 * 409 conflict raised when registration or profile editing uses an existing username.
 */
public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}
