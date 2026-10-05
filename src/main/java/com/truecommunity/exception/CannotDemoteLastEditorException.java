package com.truecommunity.exception;

/**
 * 409 conflict raised when demotion would remove the last editor account.
 */
public class CannotDemoteLastEditorException extends RuntimeException {

    public CannotDemoteLastEditorException(String message) {
        super(message);
    }
}
