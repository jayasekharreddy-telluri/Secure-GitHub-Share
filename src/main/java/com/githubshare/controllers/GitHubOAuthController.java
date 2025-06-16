package com.githubshare.controllers;

import com.githubshare.dto.BranchDTO;
import com.githubshare.dto.RepoDTO;
import com.githubshare.exceptions.InvalidRequestException;
import com.githubshare.services.GitHubOAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class GitHubOAuthController {

    private static final Logger logger = LoggerFactory.getLogger(GitHubOAuthController.class);

    private final GitHubOAuthService gitHubOAuthServiceImpl;

    public GitHubOAuthController(GitHubOAuthService gitHubOAuthServiceImpl) {
        this.gitHubOAuthServiceImpl = gitHubOAuthServiceImpl;
    }

    @GetMapping("/auth/github")
    public ResponseEntity<Void> redirectToGitHub(HttpServletRequest request) {
        logger.info("Initiating GitHub OAuth redirect...");
        URI redirectUri = gitHubOAuthServiceImpl.buildGitHubAuthorizationUri(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(redirectUri);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/auth/github/callback")
    public ResponseEntity<Void> handleGitHubCallback(@RequestParam("code") String code, HttpServletRequest request) {
        logger.info("Handling GitHub OAuth callback...");

        if (code == null || code.isEmpty()) {
            throw new InvalidRequestException("Missing authorization code in callback.");
        }

        URI redirectToFrontend = gitHubOAuthServiceImpl.processGitHubCallback(code, request);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(redirectToFrontend);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/repo-search")
    public ResponseEntity<List<RepoDTO>> searchRepos(
            @RequestParam("q") String query,
            @RequestParam("shareId") String shareId) {

        if (query == null || query.isBlank()) {
            throw new InvalidRequestException("Query parameter cannot be empty");
        }
        if (shareId == null || shareId.isBlank()) {
            throw new InvalidRequestException("Share ID cannot be empty");
        }

        return gitHubOAuthServiceImpl.searchRepos(query, shareId);
    }

    @GetMapping("/repo/{shareId}")
    public ResponseEntity<?> getRepo(@PathVariable String shareId) {
        if (shareId == null || shareId.isBlank()) {
            throw new InvalidRequestException("Share ID cannot be null or empty");
        }
        return gitHubOAuthServiceImpl.getSharedRepo(shareId);
    }

    @GetMapping("/repo/{shareId}/branches")
    public ResponseEntity<List<BranchDTO>> getBranchesByShareIdAndRepo(
            @PathVariable String shareId,
            @RequestParam("repo") String repo) {

        if (shareId.isBlank() || repo.isBlank()) {
            throw new InvalidRequestException("shareId and repo are required");
        }

        List<BranchDTO> branches = gitHubOAuthServiceImpl.getBranchesForRepo(shareId, repo);
        return ResponseEntity.ok(branches);
    }



}
