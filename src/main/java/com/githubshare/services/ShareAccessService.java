package com.githubshare.services;

import com.githubshare.dto.RepoDetailsResponse;
import com.githubshare.entity.SharedRepoLink;
import com.githubshare.repos.SharedRepoLinkRepository;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShareAccessService {

    @Autowired
    private SharedRepoLinkRepository linkRepository;

    public RepoDetailsResponse getRepoDetails(String shareId, String password) throws IOException {
        Optional<SharedRepoLink> optionalLink = linkRepository.findByShareId(shareId);

        if (optionalLink.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired share link.");
        }

        SharedRepoLink link = optionalLink.get(); // ✅ Now you can access fields

        if (link.getExpiresAt() != null && LocalDateTime.now().isAfter(link.getExpiresAt())) {
            throw new IllegalArgumentException("This share link has expired.");
        }

        if (link.isPasswordEnabled()) {
            if (password == null || !BCrypt.checkpw(password, link.getPasswordHash())) {
                throw new IllegalArgumentException("Incorrect password.");
            }
        }

        GitHub github = new GitHubBuilder().withOAuthToken(link.getGithubToken()).build();
        GHRepository repo = github.getRepository(link.getRepoOwner() + "/" + link.getRepoName());

        RepoDetailsResponse response = new RepoDetailsResponse();
        response.setRepoName(repo.getName());
        response.setOwner(repo.getOwnerName());
        response.setDescription(repo.getDescription());
        response.setPrivate(repo.isPrivate());

        List<GHContent> contents = repo.getDirectoryContent("");
        List<String> fileNames = contents.stream()
                .map(GHContent::getName)
                .collect(Collectors.toList());

        response.setFiles(fileNames);
        return response;
    }
}
