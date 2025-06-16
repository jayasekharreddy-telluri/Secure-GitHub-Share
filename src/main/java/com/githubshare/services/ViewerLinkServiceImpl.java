package com.githubshare.services;

import com.githubshare.dto.*;
import com.githubshare.entity.SharedRepoLink;
import com.githubshare.entity.ViewerLink;
import com.githubshare.exceptions.*;
import com.githubshare.repos.SharedRepoLinkRepository;
import com.githubshare.repos.ViewerLinkRepository;
import com.githubshare.utils.EncryptionUtils;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class ViewerLinkServiceImpl implements ViewerLinkService {

    private static final Logger logger = LoggerFactory.getLogger(ViewerLinkServiceImpl.class);

    private final ViewerLinkRepository viewerLinkRepository;
    private final SharedRepoLinkRepository sharedRepoLinkRepository;
    private final GitHubOAuthServiceImpl gitHubServiceImpl;
    private final RestTemplate restTemplate;

    @Value("${app.viewer.base-url}")
    private String viewerBaseUrl;

    public ViewerLinkServiceImpl(ViewerLinkRepository viewerLinkRepository,
                                 SharedRepoLinkRepository sharedRepoLinkRepository,
                                 GitHubOAuthServiceImpl gitHubServiceImpl,
                                 RestTemplate restTemplate) {
        this.viewerLinkRepository = viewerLinkRepository;
        this.sharedRepoLinkRepository = sharedRepoLinkRepository;
        this.gitHubServiceImpl = gitHubServiceImpl;
        this.restTemplate = restTemplate;
    }
    @Override
    @Transactional
    public void createViewerLink(ViewerLinkRequest request) {
        logger.info("Attempting to create viewer link for repo: {}", request.getRepoUrl());

        SharedRepoLink sharedRepo = sharedRepoLinkRepository.findByShareId(request.getShareId())
                .orElseThrow(() -> new InvalidRequestException("Invalid shareId: " + request.getShareId()));

        String repoName = request.getRepoUrl(); // This is just the name like "private_repo_three"
        String repoOwner = sharedRepo.getRepoOwner();

        // Get full GitHub repo URL from sharedRepo.repos map
        String fullRepoUrl = sharedRepo.getRepos().get(repoName);
        if (fullRepoUrl == null) {
            throw new InvalidRequestException("This repo is not included in the shared mapping.");
        }

        // Token decryption
        String decryptedToken;
        try {
            decryptedToken = EncryptionUtils.decrypt(sharedRepo.getGithubToken());
        } catch (Exception e) {
            logger.error("Token decryption failed for shareId: {}", request.getShareId(), e);
            throw new ExternalServiceException("Failed to decrypt GitHub token");
        }

        // Check if it's a private repo
        boolean isPrivate = isRepoPrivate(fullRepoUrl, decryptedToken);
        if (!isPrivate) {
            logger.warn("Repository is public or inaccessible: {}", fullRepoUrl);
            throw new InvalidRequestException("Repository must be private");
        }

        String viewerId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(request.getExpiresInMinutes());

        ViewerLink viewerLink = new ViewerLink();
        viewerLink.setViewerId(viewerId);
        viewerLink.setShareId(request.getShareId());
        viewerLink.setRepoUrl(fullRepoUrl);
        viewerLink.setMaxViews(request.getMaxViews());
        viewerLink.setViewsLeft(request.getMaxViews());
        viewerLink.setExpiresAt(expiresAt);
        viewerLink.setCreatedAt(LocalDateTime.now());
        viewerLink.setDeleted(false);
        viewerLink.setBranchName(request.getBranchName());

        viewerLinkRepository.save(viewerLink);
        logger.info("Viewer link successfully created: {}", viewerId);
    }


    private String[] parseRepoUrl(String repoUrl) {
        String cleanedUrl = repoUrl.replace(".git", "").trim();
        String[] parts = cleanedUrl.split("/");
        if (parts.length < 2) {
            throw new InvalidRequestException("Invalid GitHub repository URL: " + repoUrl);
        }
        return new String[]{parts[parts.length - 2], parts[parts.length - 1]};
    }

    private boolean isRepoPrivate(String repoUrl, String accessToken) {
        try {
            String[] parts = parseRepoUrl(repoUrl);
            String owner = parts[0];
            String repoName = parts[1];

            String apiUrl = "https://api.github.com/repos/" + owner + "/" + repoName;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/vnd.github+json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, Map.class);
            Object isPrivate = response.getBody().get("private");

            return isPrivate instanceof Boolean && (Boolean) isPrivate;
        } catch (HttpClientErrorException.NotFound e) {
            logger.error("Repository not found: {}", repoUrl);
            throw new InvalidRequestException("Repository not found or URL is incorrect.");
        } catch (Exception e) {
            logger.error("Error while checking repo privacy for URL: {}", repoUrl, e);
            throw new ExternalServiceException("Failed to check repository privacy.");
        }
    }

    @Override
    @Transactional
    public void updateViewerLink(String viewerId, ViewerLinkUpdateRequest updateRequest) {
        ViewerLink viewerLink = viewerLinkRepository.findByViewerId(viewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Viewer link not found: " + viewerId));

        logger.info("Validating viewer link before update: {}", viewerId);


        if (viewerLink.getExpiresAt() != null && viewerLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRequestException("Cannot update: Viewer link is already expired.");
        }


        if (viewerLink.getViewsLeft() <= 0) {
            throw new InvalidRequestException("Cannot update: Viewer link has no views left.");
        }

        logger.info("Updating viewer link: {}", viewerId);

        if (updateRequest.getMaxViews() != null) {
            int usedViews = viewerLink.getMaxViews() - viewerLink.getViewsLeft();
            int newMaxViews = updateRequest.getMaxViews();
            viewerLink.setMaxViews(newMaxViews);
            viewerLink.setViewsLeft(Math.max(0, newMaxViews - usedViews));
        }

        if (updateRequest.getExpiresInMinutes() != null) {
            viewerLink.setExpiresAt(LocalDateTime.now().plusMinutes(updateRequest.getExpiresInMinutes()));
        }

        viewerLinkRepository.save(viewerLink);
        logger.info("Viewer link updated successfully: {}", viewerId);
    }


    @Override
    @Transactional
    public void deleteViewerLink(String viewerId) {
        ViewerLink viewerLink = viewerLinkRepository.findByViewerId(viewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Viewer link not found: " + viewerId));

        viewerLink.setDeleted(true);
        viewerLinkRepository.delete(viewerLink);
        logger.info("Viewer link deleted: {}", viewerId);
    }

    @Override
    public Page<ViewerLinkDTO> getAllViewerLinks(String shareId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<ViewerLink> page = viewerLinkRepository.findByShareId(shareId, pageable);

        return page.map(link -> {
            String status = (link.isDeleted() || link.getViewsLeft() <= 0 || link.getExpiresAt().isBefore(now))
                    ? "expired"
                    : "active";

            return new ViewerLinkDTO(
                    viewerBaseUrl + link.getViewerId(),
                    link.getRepoUrl(),
                    link.getViewsLeft(),
                    link.getMaxViews(),
                    link.getExpiresAt().toString(),
                    status
            );
        });
    }


    @Override
    @Transactional
    public ViewerLink verifyViewerLink(String viewerId) {
        ViewerLink link = viewerLinkRepository.findByViewerId(viewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Viewer link not found for ID: " + viewerId));

        if (link.isDeleted()) {
            throw new InvalidRequestException("This link has been deleted.");
        }

        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRequestException("This link has expired.");
        }

        if (link.getViewsLeft() <= 0) {
            throw new InvalidRequestException("View limit exceeded for this link.");
        }

        return link;
    }

    public ViewerLink getViewerLinkById(String viewerId) {
        return viewerLinkRepository.findByViewerId(viewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Viewer link not found: " + viewerId));
    }

}
