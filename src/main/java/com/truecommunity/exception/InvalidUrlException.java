package com.truecommunity.exception;

/**
 * 400 bad request raised when a user/editor-supplied URL is not a valid
 * http/https link (e.g. a {@code javascript:} scheme) and would otherwise be
 * stored and rendered into an href/src attribute.
 */
public class InvalidUrlException extends RuntimeException {

    public InvalidUrlException(String message) {
        super(message);
    }
}
