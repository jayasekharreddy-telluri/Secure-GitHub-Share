package com.githubshare.repos;

import com.githubshare.entity.ViewerLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ViewerLinkRepository extends JpaRepository<ViewerLink, Long> {
    Optional<ViewerLink> findByViewerId(String viewerId);
    List<ViewerLink> findByShareId(String shareId);
    Page<ViewerLink> findByShareId(String shareId, Pageable pageable);


}
