package com.githubshare.services;

import com.githubshare.dto.RepoDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;

public interface GitHubOAuthService {
    URI buildGitHubAuthorizationUri(HttpServletRequest request);
    URI processGitHubCallback(String code, HttpServletRequest request);
    ResponseEntity<List<RepoDTO>> searchRepos(String query, String shareId);
    ResponseEntity<?> getSharedRepo(String shareId);
}
