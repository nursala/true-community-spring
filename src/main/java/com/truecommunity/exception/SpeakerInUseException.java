package com.truecommunity.exception;

/**
 * 409 conflict raised when deleting a speaker referenced by statements.
 */
public class SpeakerInUseException extends RuntimeException {

    public SpeakerInUseException(String message) {
        super(message);
    }
}
