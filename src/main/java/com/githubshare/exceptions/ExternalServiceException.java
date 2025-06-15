package com.githubshare.exceptions;

public class ExternalServiceException extends RuntimeException {
    private final String errorCode;
    private final String service;

    public ExternalServiceException(String message) {
        this(message, null, null);
    }

    public ExternalServiceException(String message, Throwable cause) {
        this(message, cause, null, null);
    }

    public ExternalServiceException(String message, String errorCode, String service) {
        super(message);
        this.errorCode = errorCode;
        this.service = service;
    }

    public ExternalServiceException(String message, Throwable cause, String errorCode, String service) {
        super(message, cause);
        this.errorCode = errorCode;
        this.service = service;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getService() {
        return service;
    }
}
