package com.githubshare.repos;


import com.githubshare.entity.SharedRepoLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface SharedRepoLinkRepository extends JpaRepository<SharedRepoLink, Long> {
    Optional<SharedRepoLink> findByShareId(String shareId);

}
