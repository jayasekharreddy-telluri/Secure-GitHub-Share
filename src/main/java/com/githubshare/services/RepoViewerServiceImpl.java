package com.githubshare.services;

import com.githubshare.dto.FileNodeDTO;
import com.githubshare.entity.ViewerLink;
import com.githubshare.exceptions.InvalidRequestException;
import com.githubshare.repos.ViewerLinkRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class RepoViewerServiceImpl implements RepoViewerService {

    private static final Logger logger = LoggerFactory.getLogger(RepoViewerServiceImpl.class);

    private final ViewerLinkService viewerLinkService;
    private final GitHubRepoReaderService gitHubRepoReaderServiceImpl;
    private final ViewerLinkRepository viewerLinkRepository;

    public RepoViewerServiceImpl(ViewerLinkService viewerLinkService,
                                 GitHubRepoReaderService gitHubRepoReaderServiceImpl, ViewerLinkRepository viewerLinkRepository) {
        this.viewerLinkService = viewerLinkService;
        this.gitHubRepoReaderServiceImpl = gitHubRepoReaderServiceImpl;
        this.viewerLinkRepository = viewerLinkRepository;
    }

    @Override
    @Transactional
    public List<FileNodeDTO> getRepoFileTree(String viewerId) {
        logger.info("Fetching file tree for viewerId: {}", viewerId);

       // ViewerLink viewerLink = viewerLinkService.getViewerLinkById(viewerId);

        ViewerLink viewerLink = viewerLinkService.verifyViewerLink(viewerId);

        viewerLink.setViewsLeft(viewerLink.getViewsLeft() - 1);

        viewerLinkRepository.save(viewerLink);

        logger.debug("ViewerLink found for repoUrl: {}", viewerLink.getRepoUrl());

        String[] parts = parseGitHubRepoUrl(viewerLink.getRepoUrl());
        logger.debug("Parsed owner: {}, repo: {}", parts[0], parts[1]);

        return gitHubRepoReaderServiceImpl.getFileTree(parts[0], parts[1], viewerLink.getShareId(),viewerLink.getBranchName());
    }

    @Override
    @Transactional
    public String getFileContent(String viewerId, String path) {
        logger.info("Fetching file content for viewerId: {}, path: {}", viewerId, path);

        ViewerLink viewerLink = viewerLinkService.verifyViewerLink(viewerId);
        logger.debug("ViewerLink retrieved with URL: {}", viewerLink.getRepoUrl());

        String[] parts = parseGitHubRepoUrl(viewerLink.getRepoUrl());
        logger.debug("Parsed owner: {}, repo: {}", parts[0], parts[1]);

        return gitHubRepoReaderServiceImpl.getFileContent(parts[0], parts[1], path, viewerLink.getShareId(),viewerLink.getBranchName());
    }

    private String[] parseGitHubRepoUrl(String repoUrl) {
        if (!StringUtils.hasText(repoUrl)) {
            throw new InvalidRequestException("Repository URL is empty");
        }

        String cleanUrl = repoUrl.trim()
                .replace("https://github.com/", "")
                .replace(".git", "");

        String[] parts = cleanUrl.split("/");

        if (parts.length != 2) {
            throw new InvalidRequestException("Invalid GitHub repository URL format: " + repoUrl);
        }

        return parts;
    }
}
