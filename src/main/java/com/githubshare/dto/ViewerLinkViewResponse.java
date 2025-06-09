package com.githubshare.dto;

public record ViewerLinkViewResponse(
        String viewerUrl,
        String repoName,
        String expiresAt,
        int maxViews
) {}
