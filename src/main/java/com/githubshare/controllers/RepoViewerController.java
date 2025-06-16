package com.githubshare.controllers;

import com.githubshare.dto.FileNodeDTO;
import com.githubshare.exceptions.InvalidRequestException;
import com.githubshare.services.RepoViewerService;
import com.githubshare.services.RepoViewerServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repo")
public class RepoViewerController {

    private final RepoViewerService repoViewerServiceImpl;

    public RepoViewerController(RepoViewerService repoViewerServiceImpl) {
        this.repoViewerServiceImpl = repoViewerServiceImpl;
    }

    @GetMapping("/viewer-file-tree")
    public ResponseEntity<List<FileNodeDTO>> getFileTree(@RequestParam("viewerId") String viewerId) {
        if (!StringUtils.hasText(viewerId)) {
            throw new InvalidRequestException("viewerId must not be blank");
        }

        List<FileNodeDTO> fileTree = repoViewerServiceImpl.getRepoFileTree(viewerId);
        return ResponseEntity.ok(fileTree);
    }

    @GetMapping("/file-content")
    public ResponseEntity<String> getFileContent(
            @RequestParam("viewerId") String viewerId,
            @RequestParam("path") String path
    ) {
        if (!StringUtils.hasText(viewerId)) {
            throw new InvalidRequestException("viewerId must not be blank");
        }
        if (!StringUtils.hasText(path)) {
            throw new InvalidRequestException("path must not be blank");
        }

        String content = repoViewerServiceImpl.getFileContent(viewerId, path);
        return ResponseEntity.ok(content);
    }
}
