package com.truecommunity.exception;

/**
 * 404 missing-resource exception for comment lookup failures.
 */
public class CommentNotFoundException extends RuntimeException {

    public CommentNotFoundException(String message) {
        super(message);
    }
}
