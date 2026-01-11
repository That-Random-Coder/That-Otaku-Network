package com.project.content_service.repository;

import com.project.content_service.domain.entity.Share;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ShareRepository extends JpaRepository<Share, UUID> {

    @Query("SELECT COUNT(s) FROM Share s WHERE s.content.id = :contentId")
    Long countByContentId(@Param("contentId") UUID contentId);

    boolean existsByContentIdAndUserId(UUID contentId, UUID userId);
}
