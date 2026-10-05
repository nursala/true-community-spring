package com.truecommunity.exception;

/**
 * 403 authorization exception for users who cannot view or participate in a discussion.
 */
public class UnauthorizedDiscussionAccessException extends RuntimeException {

    public UnauthorizedDiscussionAccessException(String message) {
        super(message);
    }
}
