package com.githubshare.services;

import com.githubshare.dto.FileNodeDto;
import com.githubshare.entity.ViewerLink;
import com.githubshare.exceptions.InvalidRequestException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RepoViewerService {

    private final ViewerLinkService viewerLinkService;
    private final GitHubRepoViewerService gitHubRepoViewerService;

    public RepoViewerService(ViewerLinkService viewerLinkService, GitHubRepoViewerService gitHubRepoViewerService) {
        this.viewerLinkService = viewerLinkService;
        this.gitHubRepoViewerService = gitHubRepoViewerService;
    }

    public List<FileNodeDto> getRepoFileTree(String viewerId) {
        // 1. Validate and decrement views
        ViewerLink viewerLink = viewerLinkService.useViewerLink(viewerId);

        // 2. Get GitHub repo URL from DB
        String repoUrl = viewerLink.getRepoUrl();

        // 3. Extract owner and repo name (remove .git suffix if present)
        String cleanUrl = repoUrl.replace("https://github.com/", "").replace(".git", "");
        String[] parts = cleanUrl.split("/");

        if (parts.length < 2) {
            throw new InvalidRequestException("Invalid GitHub repository URL");
        }

        String owner = parts[0];
        String repo = parts[1];

        // 4. Call GitHub API to get the file tree
        return gitHubRepoViewerService.getFileTree(owner, repo,viewerLink.getShareId());
    }
}
