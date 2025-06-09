package com.githubshare.controllers;

import com.githubshare.dto.*;
import com.githubshare.services.ViewerLinkService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/viewer-links")
@CrossOrigin(origins = "http://localhost:4200")
public class ViewerLinkController {

    private final ViewerLinkService viewerLinkService;

    public ViewerLinkController(ViewerLinkService viewerLinkService) {
        this.viewerLinkService = viewerLinkService;
    }

    @PostMapping("/create")
    public ResponseEntity<SuccessDTO> createViewerLink(@RequestBody ViewerLinkRequest request) {
        viewerLinkService.createViewerLink(request);
        return ResponseEntity.ok(new SuccessDTO("Viewer link created successfully"));
    }

    @PutMapping("/{viewerId}")
    public ResponseEntity<SuccessDTO> updateViewerLink(@PathVariable String viewerId,
                                                       @RequestBody ViewerLinkUpdateRequest updateRequest) {
        viewerLinkService.updateViewerLink(viewerId, updateRequest);
        return ResponseEntity.ok(new SuccessDTO("Viewer link updated successfully"));
    }

    @DeleteMapping("/{viewerId}")
    public ResponseEntity<SuccessDTO> deleteViewerLink(@PathVariable String viewerId) {
        viewerLinkService.deleteViewerLink(viewerId);
        return ResponseEntity.ok(new SuccessDTO("Viewer link marked as deleted"));
    }

    @GetMapping("/viewer/{viewerId}")
    public ResponseEntity<ViewerLinkAccessDTO> viewRepository(@PathVariable String viewerId) {
        ViewerLinkAccessDTO accessDTO = viewerLinkService.accessRepository(viewerId);
        return ResponseEntity.ok(accessDTO);
    }

    @GetMapping("/content/{viewerId}")
    public ResponseEntity<ViewerLinkContentResponse> getViewerContent(@PathVariable String viewerId) {
        ViewerLinkContentResponse contentResponse = viewerLinkService.getViewerContent(viewerId);
        return ResponseEntity.ok(contentResponse);
    }

    @GetMapping
    public ResponseEntity<Page<ViewerLinkDTO>> getAllViewerLinks(@PageableDefault(size = 5) Pageable pageable) {
        Page<ViewerLinkDTO> page = viewerLinkService.getAllViewerLinks(pageable);
        return ResponseEntity.ok(page);
    }
}
