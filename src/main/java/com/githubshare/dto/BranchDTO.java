package com.githubshare.dto;


public class BranchDTO {
    private String name;
    private String commitSha;

    public BranchDTO() {
    }

    public BranchDTO(String name, String commitSha) {
        this.name = name;
        this.commitSha = commitSha;
    }

    public String getName() {
        return name;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }
}
