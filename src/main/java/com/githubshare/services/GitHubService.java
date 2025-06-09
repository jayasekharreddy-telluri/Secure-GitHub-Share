package com.githubshare.services;



import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@Service
public class GitHubService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);

    @Value("${app.encryption.secret}")
    private String encryptionSecret;

    private final RestTemplate restTemplate;

    public GitHubService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String decryptToken(String encryptedToken) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(encryptionSecret.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decoded = Base64.getDecoder().decode(encryptedToken);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public boolean isRepoPrivate(String repoUrl, String accessToken) {
        try {
            // Extract owner and repo name from URL
            // Example repoUrl: https://github.com/owner/repo.git or https://github.com/owner/repo
            String cleanedUrl = repoUrl.endsWith(".git") ?
                    repoUrl.substring(0, repoUrl.length() - 4) : repoUrl;
            String[] parts = cleanedUrl.split("/");
            if (parts.length < 2) {
                logger.warn("Invalid repo URL format: {}", repoUrl);
                return false;
            }
            String owner = parts[parts.length - 2];
            String repo = parts[parts.length - 1];

            String apiUrl = String.format("https://api.github.com/repos/%s/%s", owner, repo);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                logger.warn("GitHub API call failed or returned no data for repo: {}", repoUrl);
                return false;
            }

            Object privateFlag = response.getBody().get("private");
            if (privateFlag instanceof Boolean) {
                return (Boolean) privateFlag;
            }

            return false;
        } catch (Exception e) {
            logger.error("Error checking repo privacy for URL {}: {}", repoUrl, e.getMessage());
            return false;
        }
    }

    public Object getReadOnlyRepoContent(String repoUrl, String accessToken) throws Exception {
        try {
            // Clean up the repoUrl to get the format: owner/repo
            String cleanedRepoPath = repoUrl
                    .replace("https://github.com/", "")
                    .replace(".git", "");

            String apiUrl = "https://api.github.com/repos/" + cleanedRepoPath + "/contents/";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    Object.class
            );

            return response.getBody();
        } catch (HttpClientErrorException e) {
            logger.error("GitHub API error: {}", e.getMessage());
            throw new Exception("Failed to fetch repo content", e);
        }
    }


}
