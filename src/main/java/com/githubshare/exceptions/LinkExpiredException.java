package com.githubshare.exceptions;

public class LinkExpiredException extends RuntimeException {
    private final String linkId;

    public LinkExpiredException(String message) {
        this(message, null);
    }

    public LinkExpiredException(String message, String linkId) {
        super(message);
        this.linkId = linkId;
    }

    public String getLinkId() {
        return linkId;
    }
}
