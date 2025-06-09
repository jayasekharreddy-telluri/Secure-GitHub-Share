package com.githubshare.dto;

public class ViewerLinkUpdateRequest {
    private Integer maxViews;
    private Integer expiresInMinutes;

    // Default constructor
    public ViewerLinkUpdateRequest() {}

    // Getters
    public Integer getMaxViews() {
        return maxViews;
    }

    public Integer getExpiresInMinutes() {
        return expiresInMinutes;
    }

    // Setters
    public void setMaxViews(Integer maxViews) {
        this.maxViews = maxViews;
    }

    public void setExpiresInMinutes(Integer expiresInMinutes) {
        this.expiresInMinutes = expiresInMinutes;
    }
}
