package com.githubshare.dto;

import java.util.List;

public class RepoDetailsResponse {
    private String repoName;
    private String owner;
    private String description;
    private boolean isPrivate;
    private List<String> files;

    // Getters and Setters
    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }

    public List<String> getFiles() { return files; }
    public void setFiles(List<String> files) { this.files = files; }
}
