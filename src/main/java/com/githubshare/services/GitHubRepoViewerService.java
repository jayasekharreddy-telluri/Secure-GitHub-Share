package com.githubshare.services;

import com.githubshare.dto.FileNodeDto;
import com.githubshare.entity.SharedRepoLink;
import com.githubshare.exceptions.InvalidRequestException;
import com.githubshare.repos.SharedRepoLinkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class GitHubRepoViewerService {

    private final SharedRepoLinkRepository sharedRepoLinkRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.encryption.secret}")
    private String encryptionSecret;

    public GitHubRepoViewerService(SharedRepoLinkRepository sharedRepoLinkRepository) {
        this.sharedRepoLinkRepository = sharedRepoLinkRepository;
    }

    public List<FileNodeDto> getFileTree(String owner, String repoName, String shareId) {
        // 1. Fetch shared repo link
        SharedRepoLink sharedRepoLink = sharedRepoLinkRepository.findByShareId(shareId)
                .orElseThrow(() -> new InvalidRequestException("Invalid shareId: " + shareId));

        // 2. Validate owner match
        if (!sharedRepoLink.getRepoOwner().equalsIgnoreCase(owner)) {
            throw new InvalidRequestException("Repo owner mismatch: expected " + sharedRepoLink.getRepoOwner() + ", got " + owner);
        }

        // 3. Validate repo exists
        String repoUrl = sharedRepoLink.getRepos().get(repoName);
        if (repoUrl == null) {
            throw new InvalidRequestException("Repository '" + repoName + "' not found in shared list for owner: " + owner);
        }

        // 4. Decrypt GitHub token
        String accessToken;
        try {
            accessToken = decryptToken(sharedRepoLink.getGithubToken());
        } catch (Exception e) {
            throw new InvalidRequestException("Failed to decrypt GitHub token: " + e.getMessage());
        }

        // 5. GitHub API call to fetch file tree
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents", owner, repoName);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<List> response;
        try {
            response = restTemplate.exchange(apiUrl, HttpMethod.GET, request, List.class);
        } catch (Exception e) {
            throw new InvalidRequestException("GitHub API error while fetching file tree: " + e.getMessage());
        }

        List<Map<String, Object>> items = response.getBody();
        if (items == null) {
            return Collections.emptyList();
        }

        List<FileNodeDto> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            FileNodeDto node = new FileNodeDto();
            node.setName((String) item.get("name"));
            node.setPath((String) item.get("path"));
            node.setDirectory("dir".equals(item.get("type")));
            node.setChildren(new ArrayList<>());
            result.add(node);
        }

        return result;
    }

    private String decryptToken(String encryptedToken) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(encryptionSecret.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decoded = Base64.getDecoder().decode(encryptedToken);
        return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
    }
}
