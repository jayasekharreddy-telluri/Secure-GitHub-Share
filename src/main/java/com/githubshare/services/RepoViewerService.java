package com.githubshare.services;

import com.githubshare.dto.FileNodeDTO;

import java.util.List;

public interface RepoViewerService {
    List<FileNodeDTO> getRepoFileTree(String viewerId);
    String getFileContent(String viewerId, String path);
}
