package com.truecommunity.exception;

/**
 * 409 conflict raised when registration uses an email that already exists.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
