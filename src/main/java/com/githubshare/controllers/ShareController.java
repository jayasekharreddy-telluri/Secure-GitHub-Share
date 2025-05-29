package com.githubshare.controllers;

import com.githubshare.dto.ShareRequest;
import com.githubshare.dto.ShareResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.githubshare.services.ShareService;

import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
public class ShareController {

    private static final Logger logger = Logger.getLogger(ShareController.class.getName());

    @Autowired
    private ShareService shareService;

    @PostMapping("/share")
    public ResponseEntity<ShareResponse> createShareLink(@RequestBody ShareRequest request) {
        logger.info("Received request to share repository.");
        ShareResponse response = shareService.createShareLink(request);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Controller is working!");
    }

}
