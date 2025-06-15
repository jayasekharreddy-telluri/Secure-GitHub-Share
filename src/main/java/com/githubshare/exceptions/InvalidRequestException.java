package com.githubshare.exceptions;

public class InvalidRequestException extends RuntimeException {
    private final String errorCode;
    private final String field;

    public InvalidRequestException(String message) {
        this(message, null, null);
    }

    public InvalidRequestException(String message, String errorCode) {
        this(message, errorCode, null);
    }

    public InvalidRequestException(String message, String errorCode, String field) {
        super(message);
        this.errorCode = errorCode;
        this.field = field;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getField() {
        return field;
    }
}
