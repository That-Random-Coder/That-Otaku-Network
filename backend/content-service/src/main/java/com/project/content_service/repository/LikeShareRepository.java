package com.project.content_service.repository;

import com.project.content_service.domain.entity.LikeShare;
import com.project.content_service.domain.enums.LikeOrDislikeEnums;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LikeShareRepository extends JpaRepository<LikeShare, UUID> {

    @Query("SELECT ls FROM LikeShare ls WHERE ls.content.id = :contentId AND ls.userId = :userId")
    Optional<LikeShare> findByContentIdAndUserId(@Param("contentId") UUID contentId, @Param("userId") UUID userId);

    boolean existsByContentIdAndUserId(UUID contentId, UUID userId);

    @Modifying
    @Query("DELETE FROM LikeShare ls WHERE ls.content.id = :contentId AND ls.userId = :userId")
    int deleteByContentIdAndUserId(@Param("contentId") UUID contentId, @Param("userId") UUID userId);
}
