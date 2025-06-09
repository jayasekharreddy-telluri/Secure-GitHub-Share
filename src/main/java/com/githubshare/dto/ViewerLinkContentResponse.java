package com.githubshare.dto;

public record ViewerLinkContentResponse(String repoUrl, String expiresAt, int viewsLeft, Object content) {}
