package com.githubshare.services;

import com.githubshare.dto.FileNodeDTO;
import com.githubshare.entity.SharedRepoLink;
import com.githubshare.exceptions.InvalidRequestException;
import com.githubshare.repos.SharedRepoLinkRepository;
import com.githubshare.utils.EncryptionUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class GitHubRepoReaderServiceImpl implements GitHubRepoReaderService {

    private final SharedRepoLinkRepository sharedRepoLinkRepository;
    private final RestTemplate restTemplate;

    public GitHubRepoReaderServiceImpl(SharedRepoLinkRepository sharedRepoLinkRepository,
                                       RestTemplate restTemplate) {
        this.sharedRepoLinkRepository = sharedRepoLinkRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileNodeDTO> getFileTree(String owner, String repoName, String shareId, String branchName) {
        SharedRepoLink sharedRepoLink = sharedRepoLinkRepository.findByShareId(shareId)
                .orElseThrow(() -> new InvalidRequestException("Invalid shareId: " + shareId));

        if (!sharedRepoLink.getRepoOwner().equalsIgnoreCase(owner)) {
            throw new InvalidRequestException("Repo owner mismatch: expected " + sharedRepoLink.getRepoOwner() + ", got " + owner);
        }

        String repoUrl = sharedRepoLink.getRepos().get(repoName);
        if (repoUrl == null) {
            throw new InvalidRequestException("Repository '" + repoName + "' not found in shared list for owner: " + owner);
        }

        String branch = (branchName == null || branchName.isBlank()) ? "main" : branchName;
        String accessToken = decryptToken(sharedRepoLink);

        String apiUrl = String.format(
                "https://api.github.com/repos/%s/%s/git/trees/%s?recursive=1",
                owner, repoName, branch
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(apiUrl, HttpMethod.GET, request, Map.class);
        } catch (Exception e) {
            throw new InvalidRequestException("GitHub Tree API error: " + e.getMessage());
        }

        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("tree")) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> tree = (List<Map<String, Object>>) body.get("tree");
        return buildFileTreeFromFlatList(tree);
    }

    private List<FileNodeDTO> buildFileTreeFromFlatList(List<Map<String, Object>> flatList) {
        Map<String, FileNodeDTO> pathMap = new HashMap<>();
        FileNodeDTO root = new FileNodeDTO("", "", true, new ArrayList<>());

        for (Map<String, Object> node : flatList) {
            String path = (String) node.get("path");
            String type = (String) node.get("type");
            boolean isFolder = "tree".equals(type);

            FileNodeDTO fileNode = new FileNodeDTO(
                    path.substring(path.lastIndexOf('/') + 1),
                    path,
                    isFolder,
                    isFolder ? new ArrayList<>() : null
            );

            pathMap.put(path, fileNode);

            if (!path.contains("/")) {
                root.getChildren().add(fileNode);
            } else {
                String parentPath = path.substring(0, path.lastIndexOf('/'));
                FileNodeDTO parent = pathMap.get(parentPath);
                if (parent != null && parent.getChildren() != null) {
                    parent.getChildren().add(fileNode);
                }
            }
        }

        return root.getChildren();
    }

    @Override
    @Transactional(readOnly = true)
    public String getFileContent(String owner, String repoName, String path, String shareId, String branchName) {
        SharedRepoLink sharedRepoLink = sharedRepoLinkRepository.findByShareId(shareId)
                .orElseThrow(() -> new InvalidRequestException("Invalid shareId: " + shareId));

        if (!sharedRepoLink.getRepoOwner().equalsIgnoreCase(owner)) {
            throw new InvalidRequestException("Repo owner mismatch: expected " + sharedRepoLink.getRepoOwner() + ", got " + owner);
        }

        String repoUrl = sharedRepoLink.getRepos().get(repoName);
        if (repoUrl == null) {
            throw new InvalidRequestException("Repository '" + repoName + "' not found in shared list for owner: " + owner);
        }

        String branch = (branchName == null || branchName.isBlank()) ? "main" : branchName;
        String accessToken = decryptToken(sharedRepoLink);

        String apiUrl = String.format(
                "https://api.github.com/repos/%s/%s/contents/%s?ref=%s",
                owner, repoName, path, branch
        );

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

            String cleanBase64 = encodedContent.replaceAll("\\s", "");
            byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64);
            return new String(decodedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new InvalidRequestException("GitHub API error while fetching file content: " + e.getMessage());
        }
    }

    private String decryptToken(SharedRepoLink link) {
        try {
            return EncryptionUtils.decrypt(link.getGithubToken());
        } catch (Exception e) {
            throw new InvalidRequestException("Failed to decrypt GitHub token: " + e.getMessage());
        }
    }
}
