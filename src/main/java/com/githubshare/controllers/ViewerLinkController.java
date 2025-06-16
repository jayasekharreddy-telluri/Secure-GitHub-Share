package com.githubshare.controllers;

import com.githubshare.dto.*;
import com.githubshare.services.ViewerLinkService;
import com.githubshare.services.ViewerLinkServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/viewer-links")
public class ViewerLinkController {

    private final ViewerLinkService viewerLinkServiceImpl;

    public ViewerLinkController(ViewerLinkService viewerLinkServiceImpl) {
        this.viewerLinkServiceImpl = viewerLinkServiceImpl;
    }

    @PostMapping("/create")
    public ResponseEntity<SuccessDTO> createViewerLink(@RequestBody ViewerLinkRequest request) {
        viewerLinkServiceImpl.createViewerLink(request);
        return ResponseEntity.ok(new SuccessDTO("Viewer link created successfully"));
    }

    @PutMapping("/{viewerId}")
    public ResponseEntity<SuccessDTO> updateViewerLink(@PathVariable String viewerId,
                                                       @RequestBody ViewerLinkUpdateRequest updateRequest) {
        viewerLinkServiceImpl.updateViewerLink(viewerId, updateRequest);
        return ResponseEntity.ok(new SuccessDTO("Viewer link updated successfully"));
    }

    @DeleteMapping("/{viewerId}")
    public ResponseEntity<SuccessDTO> deleteViewerLink(@PathVariable String viewerId) {
        viewerLinkServiceImpl.deleteViewerLink(viewerId);
        return ResponseEntity.ok(new SuccessDTO("Viewer link marked as deleted"));
    }

    @GetMapping
    public ResponseEntity<Page<ViewerLinkDTO>> getAllViewerLinks(
            @RequestHeader("X-Share-Id") String shareId,
            @PageableDefault(size = 5) Pageable pageable) {

        Page<ViewerLinkDTO> page = viewerLinkServiceImpl.getAllViewerLinks(shareId, pageable);
        return ResponseEntity.ok(page);
    }

}
