package com.truecommunity.exception;

/**
 * 409 conflict raised when voting or discussion actions are attempted after closure.
 */
public class VotingClosedException extends RuntimeException {

    public VotingClosedException(String message) {
        super(message);
    }
}
