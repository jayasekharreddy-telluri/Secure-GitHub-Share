package com.githubshare.services;

import com.githubshare.dto.FileNodeDto;
import com.githubshare.entity.SharedRepoLink;
import com.githubshare.exceptions.InvalidRequestException;
import com.githubshare.repos.SharedRepoLinkRepository;
import com.githubshare.utils.EncryptionUtils;
import org.springframework.beans.factory.annotation.Value;
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
}
