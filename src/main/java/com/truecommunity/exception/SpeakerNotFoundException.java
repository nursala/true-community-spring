package com.truecommunity.exception;

/**
 * 404 missing-resource exception for speaker lookup failures.
 */
public class SpeakerNotFoundException extends RuntimeException {

    public SpeakerNotFoundException(String message) {
        super(message);
    }
}
