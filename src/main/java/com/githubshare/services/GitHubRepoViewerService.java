package com.githubshare.services;

import com.githubshare.dto.FileNodeDto;
import com.githubshare.entity.SharedRepoLink;
import com.githubshare.exceptions.InvalidRequestException;
import com.githubshare.repos.SharedRepoLinkRepository;
import com.githubshare.utils.EncryptionUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class GitHubRepoViewerService {

    private final SharedRepoLinkRepository sharedRepoLinkRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public GitHubRepoViewerService(SharedRepoLinkRepository sharedRepoLinkRepository) {
        this.sharedRepoLinkRepository = sharedRepoLinkRepository;
    }

    public List<FileNodeDto> getFileTree(String owner, String repoName, String shareId) {
        SharedRepoLink sharedRepoLink = sharedRepoLinkRepository.findByShareId(shareId)
                .orElseThrow(() -> new InvalidRequestException("Invalid shareId: " + shareId));

        if (!sharedRepoLink.getRepoOwner().equalsIgnoreCase(owner)) {
            throw new InvalidRequestException("Repo owner mismatch: expected " + sharedRepoLink.getRepoOwner() + ", got " + owner);
        }

        String repoUrl = sharedRepoLink.getRepos().get(repoName);
        if (repoUrl == null) {
            throw new InvalidRequestException("Repository '" + repoName + "' not found in shared list for owner: " + owner);
        }

        String accessToken;
        try {
            accessToken = EncryptionUtils.decrypt(sharedRepoLink.getGithubToken());
        } catch (Exception e) {
            throw new InvalidRequestException("Failed to decrypt GitHub token: " + e.getMessage());
        }

        // Fetch root directory
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents", owner, repoName);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<List> response;
        try {
            response = restTemplate.exchange(apiUrl, HttpMethod.GET, request, List.class);
        } catch (Exception e) {
            throw new InvalidRequestException("GitHub API error while fetching root files: " + e.getMessage());
        }

        List<Map<String, Object>> items = response.getBody();
        if (items == null) return Collections.emptyList();

        List<FileNodeDto> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            result.add(buildFileNode(item, owner, repoName, accessToken));
        }

        return result;
    }



    private FileNodeDto buildFileNode(Map<String, Object> item, String owner, String repoName, String accessToken) {
        FileNodeDto node = new FileNodeDto();
        node.setName((String) item.get("name"));
        node.setPath((String) item.get("path"));
        node.setDirectory("dir".equals(item.get("type")));

        if (node.isDirectory()) {
            node.setChildren(fetchChildren(owner, repoName, node.getPath(), accessToken));
        } else {
            node.setChildren(new ArrayList<>());
        }

        return node;
    }

    private List<FileNodeDto> fetchChildren(String owner, String repoName, String path, String accessToken) {
        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", owner, repoName, path);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, request, List.class);
            List<Map<String, Object>> items = response.getBody();
            if (items == null) return Collections.emptyList();

            List<FileNodeDto> children = new ArrayList<>();
            for (Map<String, Object> item : items) {
                children.add(buildFileNode(item, owner, repoName, accessToken));
            }

            return children;
        } catch (Exception e) {
            throw new InvalidRequestException("Error while fetching children for path '" + path + "': " + e.getMessage());
        }
    }

    public String getFileContent(String owner, String repoName, String path, String shareId) {
        SharedRepoLink sharedRepoLink = sharedRepoLinkRepository.findByShareId(shareId)
                .orElseThrow(() -> new InvalidRequestException("Invalid shareId: " + shareId));

        if (!sharedRepoLink.getRepoOwner().equalsIgnoreCase(owner)) {
            throw new InvalidRequestException("Repo owner mismatch: expected " + sharedRepoLink.getRepoOwner() + ", got " + owner);
        }

        String repoUrl = sharedRepoLink.getRepos().get(repoName);
        if (repoUrl == null) {
            throw new InvalidRequestException("Repository '" + repoName + "' not found in shared list for owner: " + owner);
        }

        String accessToken;
        try {
            accessToken = EncryptionUtils.decrypt(sharedRepoLink.getGithubToken());
        } catch (Exception e) {
            throw new InvalidRequestException("Failed to decrypt GitHub token: " + e.getMessage());
        }

        // GitHub API to get file content metadata (base64-encoded)
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents/%s", owner, repoName, path);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null || !"file".equals(body.get("type"))) {
                throw new InvalidRequestException("Requested path is not a file: " + path);
            }

            String encodedContent = (String) body.get("content");
            if (encodedContent == null) {
                throw new InvalidRequestException("No content found for file: " + path);
            }

            // ✅ Clean up base64 string before decoding
            String cleanBase64 = encodedContent.replaceAll("\\s", ""); // removes \n, \r, tabs, spaces
            byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64);
            return new String(decodedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new InvalidRequestException("GitHub API error while fetching file content: " + e.getMessage());
        }
    }



}
