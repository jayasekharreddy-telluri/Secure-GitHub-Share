package com.githubshare.services;

import com.githubshare.dto.FileNodeDTO;

import java.util.List;

public interface GitHubRepoReaderService {

    List<FileNodeDTO> getFileTree(String owner, String repoName, String shareId,String branchName);
    String getFileContent(String owner, String repoName, String path, String shareId,String branchName);
}
