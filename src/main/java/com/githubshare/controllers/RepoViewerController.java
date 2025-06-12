package com.githubshare.controllers;

import com.githubshare.dto.FileNodeDto;
import com.githubshare.services.RepoViewerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repo")
public class RepoViewerController {

    private final RepoViewerService repoViewerService;

    public RepoViewerController(RepoViewerService repoViewerService) {
        this.repoViewerService = repoViewerService;
    }

    @GetMapping("/viewer-file-tree")
    public ResponseEntity<List<FileNodeDto>> getFileTree(@RequestParam("viewerId") String viewerId) {
        List<FileNodeDto> fileTree = repoViewerService.getRepoFileTree(viewerId);
        return ResponseEntity.ok(fileTree);
    }
}
