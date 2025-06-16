package com.githubshare.services;

import com.githubshare.dto.*;
import com.githubshare.entity.ViewerLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ViewerLinkService {
    void createViewerLink(ViewerLinkRequest request);
    void updateViewerLink(String viewerId, ViewerLinkUpdateRequest updateRequest);
    void deleteViewerLink(String viewerId);
    Page<ViewerLinkDTO> getAllViewerLinks(String shareId, Pageable pageable);
    ViewerLink verifyViewerLink(String viewerId);

}
