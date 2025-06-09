package com.githubshare.controllers;

import com.githubshare.dto.RepoDto;
import com.githubshare.entity.SharedRepoLink;
import com.githubshare.repos.SharedRepoLinkRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class GitHubOAuthController {

    private static final Logger logger = LoggerFactory.getLogger(GitHubOAuthController.class);

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

    public GitHubOAuthController(RestTemplate restTemplate, SharedRepoLinkRepository sharedRepoLinkRepository) {
        this.restTemplate = restTemplate;
        this.sharedRepoLinkRepository = sharedRepoLinkRepository;
    }

    @GetMapping("/auth/github")
    public ResponseEntity<Void> redirectToGitHub(HttpServletRequest request) {
        String stateToken = UUID.randomUUID().toString();
        request.getSession().setAttribute("oauth_state", stateToken);

        String githubAuthUrl = UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "repo read:user")
                .queryParam("state", stateToken)
                .queryParam("prompt", "login")
                .toUriString();

        logger.info("Generated GitHub OAuth URL with state: {}", stateToken);
        logger.debug("Redirect URL: {}", githubAuthUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(githubAuthUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @Transactional
    @GetMapping("/auth/github/callback")
    public ResponseEntity<Void> handleGitHubCallback(@RequestParam("code") String code) throws Exception {
        logger.info("Received GitHub OAuth callback with authorization code: {}", code);

        // Step 1: Exchange code for access token
        logger.info("Exchanging code for access token...");
        String tokenUrl = "https://github.com/login/oauth/access_token";

        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Map<String, String> tokenRequestBody = new HashMap<>();
        tokenRequestBody.put("client_id", clientId);
        tokenRequestBody.put("client_secret", clientSecret);
        tokenRequestBody.put("code", code);
        tokenRequestBody.put("redirect_uri", redirectUri);

        HttpEntity<Map<String, String>> tokenRequest = new HttpEntity<>(tokenRequestBody, tokenHeaders);
        ResponseEntity<Map> tokenResponse = restTemplate.exchange(tokenUrl, HttpMethod.POST, tokenRequest, Map.class);

        if (tokenResponse.getStatusCode() != HttpStatus.OK || tokenResponse.getBody() == null) {
            logger.error("Failed to exchange code for access token. Status: {}", tokenResponse.getStatusCode());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        String accessToken = (String) tokenResponse.getBody().get("access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            logger.error("Access token missing in the response from GitHub.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        logger.info("Successfully obtained access token from GitHub.");

        // Step 2: Fetch GitHub user info
        logger.info("Fetching GitHub user profile...");
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
        userHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        ResponseEntity<Map> userResponse = restTemplate.exchange(
                "https://api.github.com/user", HttpMethod.GET, new HttpEntity<>(userHeaders), Map.class
        );

        if (userResponse.getStatusCode() != HttpStatus.OK || userResponse.getBody() == null) {
            logger.error("Failed to fetch user profile from GitHub.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        Map<String, Object> user = userResponse.getBody();
        String login = (String) user.get("login");
        if (login == null || login.isEmpty()) {
            logger.error("GitHub user login is missing.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        logger.info("Fetched GitHub user: {}", login);

        // Step 3: Fetch repositories
        logger.info("Fetching GitHub repositories...");
        ResponseEntity<List> reposResponse = restTemplate.exchange(
                "https://api.github.com/user/repos", HttpMethod.GET, new HttpEntity<>(userHeaders), List.class
        );

        if (reposResponse.getStatusCode() != HttpStatus.OK || reposResponse.getBody() == null) {
            logger.error("Failed to fetch user repositories.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        List<Map<String, Object>> reposList = reposResponse.getBody();
        Map<String, String> repoNameUrlMap = new HashMap<>();
        for (Map<String, Object> repo : reposList) {
            String name = (String) repo.get("name");
            String cloneUrl = (String) repo.get("clone_url");
            if (name != null && cloneUrl != null) {
                repoNameUrlMap.put(name, cloneUrl);
            }
        }

        logger.info("Fetched {} repositories for user {}", repoNameUrlMap.size(), login);

        // Step 4: Encrypt access token
        logger.info("Encrypting access token for secure storage...");
        String encryptedToken = encryptToken(accessToken, encryptionSecret);

        // Step 5: Save data to DB
        SharedRepoLink repoLink = new SharedRepoLink();
        String shareId = UUID.randomUUID().toString();
        repoLink.setShareId(shareId);
        repoLink.setRepoOwner(login);
        repoLink.setGithubToken(encryptedToken);
        repoLink.setCreatedAt(LocalDateTime.now());
        repoLink.setRepos(repoNameUrlMap);

        sharedRepoLinkRepository.save(repoLink);
        logger.info("Saved repo sharing info in DB with shareId: {}", shareId);

        // Step 6: Redirect to frontend
        String redirectUrl = UriComponentsBuilder.fromHttpUrl("http://localhost:4200/share")
                .queryParam("shareId", shareId)
                .build()
                .toUriString();

        logger.info("Redirecting user to frontend success page: {}", redirectUrl);

        HttpHeaders redirectHeaders = new HttpHeaders();
        redirectHeaders.setLocation(URI.create(redirectUrl));
        return new ResponseEntity<>(redirectHeaders, HttpStatus.FOUND);
    }

    private String encryptToken(String token, String secret) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    @GetMapping("/api/shared-repo-links/search")
    public ResponseEntity<List<RepoDto>> searchRepos(
            @RequestParam("q") String query,
            @RequestParam("shareId") String shareId) {

        logger.info("Searching repositories for shareId: {} with query: {}", shareId, query);

        Optional<SharedRepoLink> optionalLink = sharedRepoLinkRepository.findByShareId(shareId);
        if (optionalLink.isEmpty()) {
            logger.warn("No shared repository link found for shareId: {}", shareId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        SharedRepoLink link = optionalLink.get();
        Map<String, String> repoMap = link.getRepos();
        String owner = link.getRepoOwner();

        List<RepoDto> filteredRepos = repoMap.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase().contains(query.toLowerCase()))
                .map(entry -> new RepoDto(entry.getKey(), entry.getValue(), owner))
                .toList();

        logger.info("Found {} matching repositories for query '{}'", filteredRepos.size(), query);

        return ResponseEntity.ok(filteredRepos);
    }

    @GetMapping("/api/shared-repo/{shareId}")
    public ResponseEntity<?> getSharedRepo(@PathVariable String shareId) {
        logger.info("Fetching shared repository details for shareId: {}", shareId);

        Optional<SharedRepoLink> optional = sharedRepoLinkRepository.findByShareId(shareId);
        if (optional.isEmpty()) {
            logger.warn("No repository found for shareId: {}", shareId);
            return ResponseEntity.notFound().build();
        }

        SharedRepoLink link = optional.get();

        Map<String, Object> response = new HashMap<>();
        response.put("repoOwner", link.getRepoOwner());
        response.put("avatarUrl", "https://avatars.githubusercontent.com/" + link.getRepoOwner());
        response.put("repos", link.getRepos());

        logger.info("Successfully retrieved shared repo data for user: {}", link.getRepoOwner());

        return ResponseEntity.ok(response);
    }
}
