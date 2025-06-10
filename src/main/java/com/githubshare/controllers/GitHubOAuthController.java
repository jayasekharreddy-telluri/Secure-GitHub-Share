package com.githubshare.controllers;

import com.githubshare.dto.RepoDto;
import com.githubshare.services.GitHubService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class GitHubOAuthController {

    private static final Logger logger = LoggerFactory.getLogger(GitHubOAuthController.class);

    private final GitHubService gitHubOAuthService;

    public GitHubOAuthController(GitHubService gitHubOAuthService) {
        this.gitHubOAuthService = gitHubOAuthService;
    }

    @GetMapping("/auth/github")
    public ResponseEntity<Void> redirectToGitHub(HttpServletRequest request) {
        logger.info("Initiating GitHub OAuth redirect...");
        URI redirectUri = gitHubOAuthService.buildGitHubAuthorizationUri(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(redirectUri);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/auth/github/callback")
    public ResponseEntity<Void> handleGitHubCallback(@RequestParam("code") String code, HttpServletRequest request) throws Exception {
        logger.info("Handling GitHub OAuth callback...");
        URI redirectToFrontend = gitHubOAuthService.processGitHubCallback(code, request);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(redirectToFrontend);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/api/shared-repo-links/search")
    public ResponseEntity<List<RepoDto>> searchRepos(@RequestParam("q") String query, @RequestParam("shareId") String shareId) {
        return gitHubOAuthService.searchRepos(query, shareId);
    }

    @GetMapping("/api/shared-repo/{shareId}")
    public ResponseEntity<?> getSharedRepo(@PathVariable String shareId) {
        return gitHubOAuthService.getSharedRepo(shareId);
    }
}
