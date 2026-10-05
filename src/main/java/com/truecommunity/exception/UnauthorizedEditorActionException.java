package com.truecommunity.exception;

/**
 * 403 authorization exception for editor-only actions or editor ownership failures.
 */
public class UnauthorizedEditorActionException extends RuntimeException {

    public UnauthorizedEditorActionException(String message) {
        super(message);
    }
}
