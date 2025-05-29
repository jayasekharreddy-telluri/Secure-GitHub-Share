package com.githubshare.controllers;

import com.githubshare.dto.RepoDetailsResponse;
import com.githubshare.services.ShareAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
public class ShareAccessController {

    private static final Logger logger = Logger.getLogger(ShareAccessController.class.getName());

    @Autowired
    private ShareAccessService shareAccessService;

    // GET or POST method to fetch repo details by shareId and optional password
    @PostMapping("/{shareId}")
    public ResponseEntity<?> getSharedRepoDetails(
            @PathVariable String shareId,
            @RequestParam(required = false) String password) {

        logger.info("Received request to access shared repo with shareId: " + shareId);

        try {
            RepoDetailsResponse response = shareAccessService.getRepoDetails(shareId, password);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warning("Error accessing shared repo: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            logger.severe("GitHub API error: " + e.getMessage());
            return ResponseEntity.status(500).body("Failed to fetch repository details.");
        }
    }

    @GetMapping("/{shareId}")
    public ResponseEntity<?> getSharedRepoDetailsGet(@PathVariable String shareId) {
        return getSharedRepoDetails(shareId, null);
    }

}
