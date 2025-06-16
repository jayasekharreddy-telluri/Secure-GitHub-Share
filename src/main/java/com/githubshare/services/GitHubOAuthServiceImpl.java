package com.githubshare.services;

import com.githubshare.dto.BranchDTO;
import com.githubshare.dto.RepoDTO;
import com.githubshare.entity.SharedRepoLink;
import com.githubshare.exceptions.ExternalServiceException;
import com.githubshare.exceptions.InvalidRequestException;
import com.githubshare.exceptions.ResourceNotFoundException;
import com.githubshare.repos.SharedRepoLinkRepository;
import com.githubshare.utils.EncryptionUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class GitHubOAuthServiceImpl implements GitHubOAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubOAuthServiceImpl.class);

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Value("${github.redirect.uri}")
    private String redirectUri;

    @Value("${app.encryption.secret}")
    private String encryptionSecret;

    @Value("${frontend.redirect.success}")
    private String frontendRedirectSuccess;

    private final RestTemplate restTemplate;
    private final SharedRepoLinkRepository sharedRepoLinkRepository;

    public GitHubOAuthServiceImpl(RestTemplate restTemplate, SharedRepoLinkRepository sharedRepoLinkRepository) {
        this.restTemplate = restTemplate;
        this.sharedRepoLinkRepository = sharedRepoLinkRepository;
    }

    @Override
    public URI buildGitHubAuthorizationUri(HttpServletRequest request) {
        String stateToken = UUID.randomUUID().toString();
        request.getSession().setAttribute("oauth_state", stateToken);

        return UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "repo read:user")
                .queryParam("state", stateToken)
                .queryParam("prompt", "login")
                .build()
                .toUri();
    }

    @Override
    @Transactional
    public URI processGitHubCallback(String code, HttpServletRequest request) {
        try {
            Map<String, String> tokenRequest = Map.of(
                    "client_id", clientId,
                    "client_secret", clientSecret,
                    "code", code,
                    "redirect_uri", redirectUri
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            ResponseEntity<Map> tokenResponse = restTemplate.exchange(
                    "https://github.com/login/oauth/access_token",
                    HttpMethod.POST,
                    new HttpEntity<>(tokenRequest, headers),
                    Map.class
            );

            if (!tokenResponse.getStatusCode().is2xxSuccessful()) {
                logger.error("GitHub token exchange failed with status: {}", tokenResponse.getStatusCode());
                throw new ExternalServiceException("Failed to retrieve access token from GitHub.");
            }

            String accessToken = (String) tokenResponse.getBody().get("access_token");
            if (accessToken == null) {
                throw new InvalidRequestException("GitHub did not return an access token.");
            }

            Map userProfile;
            try {
                userProfile = restTemplate.exchange(
                        "https://api.github.com/user",
                        HttpMethod.GET,
                        new HttpEntity<>(getAuthHeaders(accessToken)),
                        Map.class
                ).getBody();
            } catch (RestClientException e) {
                throw new ExternalServiceException("Failed to fetch GitHub user profile.", e);
            }

            String login = (String) userProfile.get("login");
            if (login == null) {
                throw new ExternalServiceException("GitHub user profile did not include login info.");
            }

            ResponseEntity<List> reposResponse = restTemplate.exchange(
                    "https://api.github.com/user/repos",
                    HttpMethod.GET,
                    new HttpEntity<>(getAuthHeaders(accessToken)),
                    List.class
            );

            List<Map<String, Object>> reposList = reposResponse.getBody();
            Map<String, String> repoMap = new HashMap<>();
            for (Map<String, Object> repo : reposList) {
                repoMap.put((String) repo.get("name"), (String) repo.get("clone_url"));
            }

            String encryptedToken = EncryptionUtils.encrypt(accessToken);

            Optional<SharedRepoLink> existingLinkOpt = sharedRepoLinkRepository.findByRepoOwner(login);
            String shareId;

            if (existingLinkOpt.isPresent()) {
                SharedRepoLink existing = existingLinkOpt.get();
                existing.setGithubToken(encryptedToken);
                existing.setRepos(repoMap);
                sharedRepoLinkRepository.save(existing);
                shareId = existing.getShareId();
                logger.info("Reusing existing shareId: {}", shareId);
            } else {
                SharedRepoLink repoLink = new SharedRepoLink();
                shareId = UUID.randomUUID().toString();
                repoLink.setShareId(shareId);
                repoLink.setRepoOwner(login);
                repoLink.setGithubToken(encryptedToken);
                repoLink.setCreatedAt(LocalDateTime.now());
                repoLink.setRepos(repoMap);
                sharedRepoLinkRepository.save(repoLink);
                logger.info("Created new shareId: {}", shareId);
            }

            return UriComponentsBuilder.fromHttpUrl(frontendRedirectSuccess)
                    .queryParam("shareId", shareId)
                    .build()
                    .toUri();

        } catch (ExternalServiceException | InvalidRequestException e) {
            throw e; // already logged
        } catch (Exception ex) {
            logger.error("OAuth flow failed: {}", ex.getMessage(), ex);
            throw new ExternalServiceException("OAuth process failed. Please try again.", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<RepoDTO>> searchRepos(String query, String shareId) {
        SharedRepoLink link = sharedRepoLinkRepository.findByShareId(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Shared repository not found for ID: " + shareId));

        List<RepoDTO> filtered = link.getRepos().entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().contains(query.toLowerCase()))
                .map(e -> new RepoDTO(e.getKey(), e.getValue(), link.getRepoOwner()))
                .toList();

        return ResponseEntity.ok(filtered);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSharedRepo(String shareId) {
        SharedRepoLink link = sharedRepoLinkRepository.findByShareId(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("No shared repo found for ID: " + shareId));

        Map<String, Object> response = Map.of(
                "repoOwner", link.getRepoOwner(),
                "avatarUrl", "https://avatars.githubusercontent.com/" + link.getRepoOwner(),
                "repos", link.getRepos()
        );
        return ResponseEntity.ok(response);
    }

    private HttpHeaders getAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    @Override
    public List<BranchDTO> getBranchesForRepo(String shareId, String repo) {

        SharedRepoLink sharedRepoLink = sharedRepoLinkRepository.findByShareId(shareId)
                .orElseThrow(() -> new InvalidRequestException("Invalid shareId: " + shareId));

        String owner = sharedRepoLink.getRepoOwner();

        if (!sharedRepoLink.getRepoOwner().equalsIgnoreCase(owner)) {
            throw new InvalidRequestException("Owner mismatch.");
        }

        Map<String, String> repos = sharedRepoLink.getRepos();
        System.out.println("Repos map keys: " + repos.keySet());

        String repoUrl = repos.get(repo);
        if (repoUrl == null) {
            throw new InvalidRequestException("Repo '" + repo + "' not found for shareId: " + shareId);
        }

        String accessToken;
        try {
            accessToken = EncryptionUtils.decrypt(sharedRepoLink.getGithubToken());
        } catch (Exception e) {
            throw new InvalidRequestException("Failed to decrypt access token: " + e.getMessage());
        }

        String url = String.format("https://api.github.com/repos/%s/%s/branches", owner, repo);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<>() {}
            );

            List<Map<String, Object>> body = response.getBody();
            if (body == null) return List.of();

            List<BranchDTO> branches = new ArrayList<>();
            for (Map<String, Object> branch : body) {
                String name = (String) branch.get("name");
                Map<String, Object> commit = (Map<String, Object>) branch.get("commit");
                String sha = commit != null ? (String) commit.get("sha") : null;
                branches.add(new BranchDTO(name, sha));
            }

            return branches;

        } catch (Exception e) {
            throw new InvalidRequestException("Error fetching branches: " + e.getMessage());
        }
    }




}
