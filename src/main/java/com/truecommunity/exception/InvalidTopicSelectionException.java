package com.truecommunity.exception;

/**
 * 400 validation/input exception for missing or unknown topic selections.
 */
public class InvalidTopicSelectionException extends RuntimeException {

    public InvalidTopicSelectionException(String message) {
        super(message);
    }
}
