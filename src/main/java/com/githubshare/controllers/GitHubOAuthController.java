package com.githubshare.controllers;

import com.githubshare.entity.SharedRepoLink;
import com.githubshare.repos.SharedRepoLinkRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class GitHubOAuthController {

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Value("${github.redirect.uri}")
    private String redirectUri;

    @Value("${app.encryption.secret}")
    private String encryptionSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final SharedRepoLinkRepository sharedRepoLinkRepository;

    public GitHubOAuthController(SharedRepoLinkRepository sharedRepoLinkRepository) {
        this.sharedRepoLinkRepository = sharedRepoLinkRepository;
    }

    @GetMapping("/auth/github")
    public ResponseEntity<Void> redirectToGitHub() {
        String githubAuthUrl = UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "read:user")
                .queryParam("prompt", "login")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(UriComponentsBuilder.fromHttpUrl(githubAuthUrl).build().toUri());
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @Transactional
    @GetMapping("/auth/github/callback")
    public ResponseEntity<Void> handleGitHubCallback(@RequestParam("code") String code) throws Exception {
        // Exchange code for access token
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
        String accessToken = (String) tokenResponse.getBody().get("access_token");

        // Fetch user info
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
        userHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

        ResponseEntity<Map> userResponse = restTemplate.exchange(
                "https://api.github.com/user", HttpMethod.GET, userRequest, Map.class
        );

        Map user = userResponse.getBody();

        // Encrypt the token
        String encryptedToken = encryptToken(accessToken, encryptionSecret);

        // Save SharedRepoLink entity
        SharedRepoLink repoLink = new SharedRepoLink();
        repoLink.setShareId(UUID.randomUUID().toString());
        repoLink.setRepoOwner((String) user.get("login"));
        repoLink.setRepoName("unknown"); // Optionally fetch repo name elsewhere if needed
        repoLink.setGithubToken(encryptedToken);
        repoLink.setPasswordEnabled(false);
        repoLink.setCreatedAt(LocalDateTime.now());
        repoLink.setExpiresAt(LocalDateTime.now().plusDays(7));

        sharedRepoLinkRepository.save(repoLink);

        // Redirect to frontend with user info and shareId
        String redirectUrl = UriComponentsBuilder.fromHttpUrl("http://localhost:4200/success")
                .queryParam("name", user.get("name"))
                .queryParam("login", user.get("login"))
                .queryParam("avatar", user.get("avatar_url"))
                .queryParam("shareId", repoLink.getShareId())
                .build()
                .toUriString();

        HttpHeaders redirectHeaders = new HttpHeaders();
        redirectHeaders.setLocation(java.net.URI.create(redirectUrl));
        return new ResponseEntity<>(redirectHeaders, HttpStatus.FOUND);
    }

    private String encryptToken(String token, String secret) throws Exception {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8); // specify charset
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException("Invalid AES key length: " + keyBytes.length);
        }
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

}
