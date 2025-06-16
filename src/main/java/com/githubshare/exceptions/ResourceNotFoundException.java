package com.githubshare.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    private final String resourceName;
    private final String resourceId;

    public ResourceNotFoundException(String message) {
        this(message, null, null);
    }

    public ResourceNotFoundException(String message, String resourceName, String resourceId) {
        super(message);
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getResourceId() {
        return resourceId;
    }
}
