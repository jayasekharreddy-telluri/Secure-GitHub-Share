package com.githubshare.services;

import com.githubshare.dto.ShareRequest;
import com.githubshare.dto.ShareResponse;
import com.githubshare.entity.SharedRepoLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.githubshare.repos.SharedRepoLinkRepository;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class ShareService {

    private static final Logger logger = Logger.getLogger(ShareService.class.getName());

    @Autowired
    private SharedRepoLinkRepository repository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ShareResponse createShareLink(ShareRequest request) {
        logger.info("Creating share link for GitHub URL: " + request.getGithubUrl());

        String[] parts = request.getGithubUrl().replace("https://github.com/", "").split("/");
        String owner = parts[0];
        String repo = parts[1];

        String shareId = UUID.randomUUID().toString().substring(0, 8);
        String passwordHash = null;

        if (request.isPasswordEnabled()) {
            passwordHash = passwordEncoder.encode(request.getPassword());
            logger.info("Password protection enabled.");
        }

        SharedRepoLink link = new SharedRepoLink();
        link.setShareId(shareId);
        link.setRepoOwner(owner);
        link.setRepoName(repo);
        link.setGithubToken(request.getGithubToken());
        link.setPasswordEnabled(request.isPasswordEnabled());
        link.setPasswordHash(passwordHash);
        link.setCreatedAt(LocalDateTime.now());
        link.setExpiresAt(request.getExpiresAt());

        repository.save(link);
        logger.info("Share link created and saved with ID: " + shareId);

        return new ShareResponse("https://yourapp.com/view/" + shareId);
    }
}
