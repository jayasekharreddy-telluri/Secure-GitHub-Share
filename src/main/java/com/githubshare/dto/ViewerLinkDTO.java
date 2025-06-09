package com.githubshare.dto;

public class ViewerLinkDTO {

    private String viewerUrl;
    private String repoUrl;
    private int viewsLeft;
    private int maxViews;
    private String expiresAt;
    private String status;

    // Constructors
    public ViewerLinkDTO() {}

    public ViewerLinkDTO(String viewerUrl, String repoUrl, int viewsLeft, int maxViews, String expiresAt, String status) {
        this.viewerUrl = viewerUrl;
        this.repoUrl = repoUrl;
        this.viewsLeft = viewsLeft;
        this.maxViews = maxViews;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    // Getters and Setters
    public String getViewerUrl() {
        return viewerUrl;
    }

    public void setViewerUrl(String viewerUrl) {
        this.viewerUrl = viewerUrl;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public int getViewsLeft() {
        return viewsLeft;
    }

    public void setViewsLeft(int viewsLeft) {
        this.viewsLeft = viewsLeft;
    }

    public int getMaxViews() {
        return maxViews;
    }

    public void setMaxViews(int maxViews) {
        this.maxViews = maxViews;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
