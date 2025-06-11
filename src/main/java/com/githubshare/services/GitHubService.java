package com.githubshare.services;

import com.githubshare.dto.RepoDto;
import com.githubshare.entity.SharedRepoLink;
import com.githubshare.exceptions.ExternalServiceException;
import com.githubshare.exceptions.InvalidRequestException;
import com.githubshare.exceptions.ResourceNotFoundException;
import com.githubshare.repos.SharedRepoLinkRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class GitHubService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Value("${github.redirect.uri}")
    private String redirectUri;

    @Value("${app.encryption.secret}")
    private String encryptionSecret;

    private final RestTemplate restTemplate;
    private final SharedRepoLinkRepository sharedRepoLinkRepository;

    public GitHubService(RestTemplate restTemplate, SharedRepoLinkRepository sharedRepoLinkRepository) {
        this.restTemplate = restTemplate;
        this.sharedRepoLinkRepository = sharedRepoLinkRepository;
    }

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

    @Transactional
    public URI processGitHubCallback(String code, HttpServletRequest request) {
        try {
            logger.info("Exchanging code for token...");
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
                throw new ExternalServiceException("Failed to retrieve access token from GitHub.");
            }

            String accessToken = (String) tokenResponse.getBody().get("access_token");
            if (accessToken == null) {
                throw new InvalidRequestException("GitHub did not return an access token.");
            }

            Map userProfile = restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    new HttpEntity<>(getAuthHeaders(accessToken)),
                    Map.class
            ).getBody();

            String login = (String) userProfile.get("login");

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

            String encryptedToken = encryptToken(accessToken);

            // 🆕 Reuse logic
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

            return UriComponentsBuilder.fromHttpUrl("http://localhost:4200/share")
                    .queryParam("shareId", shareId)
                    .build()
                    .toUri();

        } catch (Exception ex) {
            logger.error("OAuth flow failed: {}", ex.getMessage(), ex);
            throw new ExternalServiceException("OAuth process failed. Please try again.");
        }
    }

    public ResponseEntity<List<RepoDto>> searchRepos(String query, String shareId) {
        SharedRepoLink link = sharedRepoLinkRepository.findByShareId(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Shared repository not found for ID: " + shareId));

        List<RepoDto> filtered = link.getRepos().entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().contains(query.toLowerCase()))
                .map(e -> new RepoDto(e.getKey(), e.getValue(), link.getRepoOwner()))
                .toList();

        return ResponseEntity.ok(filtered);
    }

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

    private String encryptToken(String token) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(encryptionSecret.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return Base64.getEncoder().encodeToString(cipher.doFinal(token.getBytes(StandardCharsets.UTF_8)));
    }

    public String decryptToken(String encryptedToken) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(encryptionSecret.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decoded = Base64.getDecoder().decode(encryptedToken);
        return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
    }
}
