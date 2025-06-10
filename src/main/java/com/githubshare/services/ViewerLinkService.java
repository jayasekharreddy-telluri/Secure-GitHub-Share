package com.githubshare.services;

import com.githubshare.dto.*;
import com.githubshare.entity.SharedRepoLink;
import com.githubshare.entity.ViewerLink;
import com.githubshare.exceptions.*;
import com.githubshare.repos.SharedRepoLinkRepository;
import com.githubshare.repos.ViewerLinkRepository;

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
public class ViewerLinkService {

    private static final Logger logger = LoggerFactory.getLogger(ViewerLinkService.class);

    private final ViewerLinkRepository viewerLinkRepository;
    private final SharedRepoLinkRepository sharedRepoLinkRepository;
    private final GitHubService gitHubService;
    private final RestTemplate restTemplate;

    @Value("${app.viewer.base-url}")
    private String viewerBaseUrl;

    public ViewerLinkService(ViewerLinkRepository viewerLinkRepository,
                             SharedRepoLinkRepository sharedRepoLinkRepository,
                             GitHubService gitHubService,
                             RestTemplate restTemplate) {
        this.viewerLinkRepository = viewerLinkRepository;
        this.sharedRepoLinkRepository = sharedRepoLinkRepository;
        this.gitHubService = gitHubService;
        this.restTemplate = restTemplate;
    }

    public void createViewerLink(ViewerLinkRequest request) {
        logger.info("Attempting to create viewer link for repo: {}", request.getRepoUrl());

        SharedRepoLink sharedRepo = sharedRepoLinkRepository.findByShareId(request.getShareId())
                .orElseThrow(() -> new InvalidRequestException("Invalid shareId: " + request.getShareId()));

        String decryptedToken;

        try {
            decryptedToken = gitHubService.decryptToken(sharedRepo.getGithubToken());
        } catch (Exception e) {
            logger.error("Token decryption failed for shareId: {}", request.getShareId(), e);
            throw new ExternalServiceException("Failed to decrypt GitHub token");
        }

        boolean isPrivate;
        try {
            isPrivate = isRepoPrivate(request.getRepoUrl(), decryptedToken);
        } catch (Exception e) {
            logger.error("Failed to check if repository is private: {}", request.getRepoUrl(), e);
            throw new ExternalServiceException("Failed to validate repository privacy");
        }

        if (!isPrivate) {
            logger.warn("Repository is public or inaccessible: {}", request.getRepoUrl());
            throw new InvalidRequestException("Repository must be private");
        }

        String viewerId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(request.getExpiresInMinutes());

        ViewerLink viewerLink = new ViewerLink();
        viewerLink.setViewerId(viewerId);
        viewerLink.setShareId(request.getShareId());
        viewerLink.setRepoUrl(request.getRepoUrl());
        viewerLink.setMaxViews(request.getMaxViews());
        viewerLink.setViewsLeft(request.getMaxViews());
        viewerLink.setExpiresAt(expiresAt);
        viewerLink.setCreatedAt(LocalDateTime.now());
        viewerLink.setDeleted(false);

        viewerLinkRepository.save(viewerLink);
        logger.info("Viewer link successfully created: {}", viewerId);
    }

    public boolean isRepoPrivate(String repoUrl, String accessToken) {
        try {
            String cleanedUrl = repoUrl.replace(".git", "").trim();
            String[] parts = cleanedUrl.split("/");
            String owner = parts[parts.length - 2];
            String repoName = parts[parts.length - 1];

            String apiUrl = "https://api.github.com/repos/" + owner + "/" + repoName;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/vnd.github+json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = new RestTemplate().exchange(apiUrl, HttpMethod.GET, entity, Map.class);

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


    private String convertToApiUrl(String repoUrl) {
        if (!repoUrl.startsWith("https://github.com/")) {
            throw new InvalidRequestException("Invalid GitHub repository URL: " + repoUrl);
        }
        String[] parts = repoUrl.replace("https://github.com/", "").split("/");
        if (parts.length < 2) {
            throw new InvalidRequestException("Malformed GitHub repository URL: " + repoUrl);
        }
        return "https://api.github.com/repos/" + parts[0] + "/" + parts[1];
    }

    public void updateViewerLink(String viewerId, ViewerLinkUpdateRequest updateRequest) {
        ViewerLink viewerLink = viewerLinkRepository.findByViewerId(viewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Viewer link not found: " + viewerId));

        logger.info("Updating viewer link: {}", viewerId);

        if (updateRequest.getMaxViews() != null) {
            int usedViews = viewerLink.getMaxViews() - viewerLink.getViewsLeft();
            viewerLink.setMaxViews(updateRequest.getMaxViews());
            viewerLink.setViewsLeft(Math.max(0, updateRequest.getMaxViews() - usedViews));
        }

        if (updateRequest.getExpiresInMinutes() != null) {
            viewerLink.setExpiresAt(LocalDateTime.now().plusMinutes(updateRequest.getExpiresInMinutes()));
        }

        viewerLinkRepository.save(viewerLink);
        logger.info("Viewer link updated: {}", viewerId);
    }

    public void deleteViewerLink(String viewerId) {
        ViewerLink viewerLink = viewerLinkRepository.findByViewerId(viewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Viewer link not found: " + viewerId));

        viewerLink.setDeleted(true);
        viewerLinkRepository.delete(viewerLink);

        logger.info("Viewer link deleted: {}", viewerId);
    }

    public ViewerLinkAccessDTO accessRepository(String viewerId) {
        ViewerLink viewerLink = viewerLinkRepository.findByViewerId(viewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Viewer link not found: " + viewerId));

        if (viewerLink.isDeleted()) {
            logger.warn("Attempt to access deleted viewer link: {}", viewerId);
            throw new LinkExpiredException("This viewer link has been deleted.");
        }

        if (viewerLink.getViewsLeft() <= 0) {
            logger.warn("Viewer link exhausted: {}", viewerId);
            throw new LinkExpiredException("No views remaining.");
        }

        if (viewerLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            logger.warn("Viewer link expired: {}", viewerId);
            throw new LinkExpiredException("Link has expired.");
        }

        viewerLink.setViewsLeft(viewerLink.getViewsLeft() - 1);
        viewerLinkRepository.saveAndFlush(viewerLink);

        logger.info("Viewer link {} accessed, remaining views: {}", viewerId, viewerLink.getViewsLeft());

        return new ViewerLinkAccessDTO(viewerLink.getRepoUrl(), viewerLink.getViewsLeft());
    }

    public Page<ViewerLinkDTO> getAllViewerLinks(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<ViewerLink> page = viewerLinkRepository.findAll(pageable);

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
}
