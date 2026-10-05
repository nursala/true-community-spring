package com.truecommunity.exception;

/**
 * 400 validation/input exception for password confirmation or current-password failures.
 */
public class PasswordMismatchException extends RuntimeException {

    public PasswordMismatchException(String message) {
        super(message);
    }
}
