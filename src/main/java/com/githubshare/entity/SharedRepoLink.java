package com.githubshare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
public class SharedRepoLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String shareId;

    private String repoOwner;

    private String githubToken;

    private LocalDateTime createdAt;


    public Map<String, String> getRepos() {
        return repos;
    }

    public void setRepos(Map<String, String> repos) {
        this.repos = repos;
    }

    @ElementCollection
    @CollectionTable(name = "shared_repo_map", joinColumns = @JoinColumn(name = "share_id"))
    @MapKeyColumn(name = "repo_name")
    @Column(name = "repo_url")
    private Map<String, String> repos;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public String getRepoOwner() {
        return repoOwner;
    }

    public void setRepoOwner(String repoOwner) {
        this.repoOwner = repoOwner;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


}
