package com.githubshare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class SharedRepoLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String shareId;

    private String repoOwner;
    private String repoName;

    private String githubToken;

    private boolean passwordEnabled;
    private String passwordHash;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getShareId() { return shareId; }
    public void setShareId(String shareId) { this.shareId = shareId; }

    public String getRepoOwner() { return repoOwner; }
    public void setRepoOwner(String repoOwner) { this.repoOwner = repoOwner; }

    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }

    public String getGithubToken() { return githubToken; }
    public void setGithubToken(String githubToken) { this.githubToken = githubToken; }

    public boolean isPasswordEnabled() { return passwordEnabled; }
    public void setPasswordEnabled(boolean passwordEnabled) { this.passwordEnabled = passwordEnabled; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
