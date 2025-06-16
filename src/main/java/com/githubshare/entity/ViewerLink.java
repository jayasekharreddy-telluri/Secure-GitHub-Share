package com.githubshare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ViewerLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String viewerId;
    private String repoUrl;
    private int maxViews;
    private int viewsLeft;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    private String shareId;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = true)
    private String branchName;

    // Constructors
    public ViewerLink() {}

    public ViewerLink(Long id, String viewerId, String repoUrl, int maxViews, int viewsLeft, LocalDateTime expiresAt,
                      LocalDateTime createdAt, String shareId, boolean deleted, String branchName) {
        this.id = id;
        this.viewerId = viewerId;
        this.repoUrl = repoUrl;
        this.maxViews = maxViews;
        this.viewsLeft = viewsLeft;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.shareId = shareId;
        this.deleted = deleted;
        this.branchName = branchName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getViewerId() {
        return viewerId;
    }

    public void setViewerId(String viewerId) {
        this.viewerId = viewerId;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public int getMaxViews() {
        return maxViews;
    }

    public void setMaxViews(int maxViews) {
        this.maxViews = maxViews;
    }

    public int getViewsLeft() {
        return viewsLeft;
    }

    public void setViewsLeft(int viewsLeft) {
        this.viewsLeft = viewsLeft;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }
}
