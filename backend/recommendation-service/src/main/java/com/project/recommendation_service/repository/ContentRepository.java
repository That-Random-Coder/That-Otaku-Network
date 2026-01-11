package com.project.recommendation_service.repository;

import com.project.recommendation_service.domain.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<Content , UUID> {

    @Modifying
    @Query("UPDATE Content c SET c.enable = :enable WHERE c.contentId = :id")
    int updateEnableById(@Param("id") UUID id , @Param("enable") Boolean enable);


    @Modifying
    @Query("UPDATE Content c SET c.dislikeCount = c.dislikeCount - 1 WHERE c.contentId = :id")
    int decreamentDislikeCount(@Param("id") UUID contentId);

    @Modifying
    @Query("UPDATE Content c SET c.likeCount = c.likeCount - 1 WHERE c.contentId = :id")
    int decreamentLikeCount(@Param("id") UUID contentId);

    @Modifying
    @Query("UPDATE Content c SET c.dislikeCount = c.dislikeCount - 1 , c.likeCount = c.likeCount + 1 WHERE c.contentId = :id")
    int dislikeToLikeCount(@Param("id") UUID contentId);

    @Modifying
    @Query("UPDATE Content c SET c.dislikeCount = c.dislikeCount + 1 , c.likeCount = c.likeCount - 1 WHERE c.contentId = :id")
    int likeToDisLikeCount(@Param("id") UUID contentId);

}
