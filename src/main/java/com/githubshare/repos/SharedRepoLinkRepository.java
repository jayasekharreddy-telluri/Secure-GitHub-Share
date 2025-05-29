package com.githubshare.repos;


import com.githubshare.entity.SharedRepoLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SharedRepoLinkRepository extends JpaRepository<SharedRepoLink, Long> {
    Optional<SharedRepoLink> findByShareId(String shareId);
}
