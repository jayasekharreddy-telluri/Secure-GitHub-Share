package com.githubshare.controllers;

import com.githubshare.dto.*;
import com.githubshare.entity.SharedRepoLink;
import com.githubshare.entity.ViewerLink;
import com.githubshare.repos.SharedRepoLinkRepository;
import com.githubshare.repos.ViewerLinkRepository;
import com.githubshare.services.GitHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/viewer-links")
@CrossOrigin(origins = "http://localhost:4200")
public class ViewerLinkController {

    private final Logger logger = LoggerFactory.getLogger(ViewerLinkController.class);

    private final ViewerLinkRepository viewerLinkRepository;
    private final SharedRepoLinkRepository sharedRepoLinkRepository;
    private final GitHubService gitHubService;

    @Value("${app.viewer.base-url}")
    private String viewerBaseUrl;

    public ViewerLinkController(ViewerLinkRepository viewerLinkRepository,
                                SharedRepoLinkRepository sharedRepoLinkRepository,
                                GitHubService gitHubService) {
        this.viewerLinkRepository = viewerLinkRepository;
        this.sharedRepoLinkRepository = sharedRepoLinkRepository;
        this.gitHubService = gitHubService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createViewerLink(@RequestBody ViewerLinkRequest request) {
        logger.info("Received request to create viewer link for repo: {}", request.getRepoUrl());

        Optional<SharedRepoLink> optionalSharedRepo = sharedRepoLinkRepository.findByShareId(request.getShareId());
        if (optionalSharedRepo.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Invalid shareId"));
        }

        try {
            String decryptedToken = gitHubService.decryptToken(optionalSharedRepo.get().getGithubToken());
            if (!gitHubService.isRepoPrivate(request.getRepoUrl(), decryptedToken)) {
                return ResponseEntity.badRequest().body(new ErrorDTO("Repository is not private"));
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

            // Return only success, no detailed DTO needed here
            return ResponseEntity.ok(new SuccessDTO("Viewer link created successfully"));
        } catch (Exception e) {
            logger.error("Error creating viewer link", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorDTO("Internal server error"));
        }
    }


    @PutMapping("/{viewerId}")
    public ResponseEntity<?> updateViewerLink(@PathVariable String viewerId, @RequestBody ViewerLinkUpdateRequest updateRequest) {
        Optional<ViewerLink> optionalViewerLink = viewerLinkRepository.findByViewerId(viewerId);
        if (optionalViewerLink.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDTO("Viewer link not found"));
        }

        ViewerLink viewerLink = optionalViewerLink.get();
        if (updateRequest.getMaxViews() != null) {
            int used = viewerLink.getMaxViews() - viewerLink.getViewsLeft();
            viewerLink.setMaxViews(updateRequest.getMaxViews());
            viewerLink.setViewsLeft(updateRequest.getMaxViews() - used);
        }

        if (updateRequest.getExpiresInMinutes() != null) {
            viewerLink.setExpiresAt(LocalDateTime.now().plusMinutes(updateRequest.getExpiresInMinutes()));
        }

        viewerLinkRepository.save(viewerLink);
        return ResponseEntity.ok(new SuccessDTO("Viewer link updated successfully"));
    }

    @DeleteMapping("/{viewerId}")
    public ResponseEntity<?> deleteViewerLink(@PathVariable String viewerId) {
        Optional<ViewerLink> optionalViewerLink = viewerLinkRepository.findByViewerId(viewerId);
        if (optionalViewerLink.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDTO("Viewer link not found"));
        }

        ViewerLink viewerLink = optionalViewerLink.get();
        viewerLink.setDeleted(true);
        viewerLinkRepository.delete(viewerLink);

        return ResponseEntity.ok(new SuccessDTO("Viewer link marked as deleted"));
    }
    @GetMapping("/viewer/{viewerId}")
    public ResponseEntity<?> viewRepository(@PathVariable String viewerId) {
        Optional<ViewerLink> optionalViewerLink = viewerLinkRepository.findByViewerId(viewerId);
        if (optionalViewerLink.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDTO("Viewer link not found"));
        }

        ViewerLink viewerLink = optionalViewerLink.get();

        if (viewerLink.isDeleted() || viewerLink.getViewsLeft() <= 0 || viewerLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.GONE).body(new ErrorDTO("Link expired or not available"));
        }

        System.out.println("Views before decrement: " + viewerLink.getViewsLeft());
        viewerLink.setViewsLeft(viewerLink.getViewsLeft() - 1);
        viewerLinkRepository.saveAndFlush(viewerLink);
        System.out.println("Views after decrement: " + viewerLink.getViewsLeft());

        return ResponseEntity.ok(new ViewerLinkAccessDTO(
                viewerLink.getRepoUrl(),
                viewerLink.getViewsLeft()
        ));
    }


    @GetMapping("/content/{viewerId}")
    public ResponseEntity<?> getViewerContent(@PathVariable String viewerId) {
        Optional<ViewerLink> optionalViewerLink = viewerLinkRepository.findByViewerId(viewerId);
        if (optionalViewerLink.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDTO("Viewer link not found"));
        }

        ViewerLink viewerLink = optionalViewerLink.get();

        if (viewerLink.isDeleted() || viewerLink.getViewsLeft() <= 0 || viewerLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.GONE).body(new ErrorDTO("Link expired or no views left"));
        }

        Optional<SharedRepoLink> optionalSharedRepo = sharedRepoLinkRepository.findByShareId(viewerLink.getShareId());
        if (optionalSharedRepo.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorDTO("Invalid shareId"));
        }

        try {
            String token = gitHubService.decryptToken(optionalSharedRepo.get().getGithubToken());
            Object content = gitHubService.getReadOnlyRepoContent(viewerLink.getRepoUrl(), token);

            viewerLink.setViewsLeft(viewerLink.getViewsLeft() - 1);
            viewerLinkRepository.save(viewerLink);

            return ResponseEntity.ok(new ViewerLinkContentResponse(
                    viewerLink.getRepoUrl(),
                    viewerLink.getExpiresAt().toString(),
                    viewerLink.getViewsLeft(),
                    content
            ));
        } catch (Exception e) {
            logger.error("Error loading GitHub content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDTO("Failed to fetch content"));
        }
    }


    @GetMapping
    public ResponseEntity<Page<ViewerLinkDTO>> getAllViewerLinks(
            @PageableDefault(size = 5) Pageable pageable // default page size 10
    ) {
        LocalDateTime now = LocalDateTime.now();

        // Fetch paginated data from DB
        Page<ViewerLink> page = viewerLinkRepository.findAll(pageable);

        // Map entities to DTOs with status logic
        Page<ViewerLinkDTO> dtoPage = page.map(link -> {
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

        return ResponseEntity.ok(dtoPage);
    }


}
