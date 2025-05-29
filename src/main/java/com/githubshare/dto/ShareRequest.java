package com.githubshare.dto;


import java.time.LocalDateTime;

public class ShareRequest {

    private String githubUrl;
    private String githubToken;
    private boolean passwordEnabled;
    private String password;
    private LocalDateTime expiresAt;

    // Getters and Setters

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public String getGithubToken() { return githubToken; }
    public void setGithubToken(String githubToken) { this.githubToken = githubToken; }

    public boolean isPasswordEnabled() { return passwordEnabled; }
    public void setPasswordEnabled(boolean passwordEnabled) { this.passwordEnabled = passwordEnabled; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
