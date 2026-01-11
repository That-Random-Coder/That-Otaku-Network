package com.project.content_service.repository;

import com.project.content_service.domain.dto.response.CommentResponse;
import com.project.content_service.domain.entity.Comments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentsRepository extends JpaRepository<Comments, UUID> {

    @Query("""
            SELECT new com.project.content_service.domain.dto.response.CommentResponse(
                c.id,
                c.comment,
                c.userId,
                c.userUserName,
                c.userUserName,
                c.commentAt
            )
            FROM Comments c
            WHERE c.content.id = :contentId
            ORDER BY c.commentAt DESC
            """)
    Page<CommentResponse> getCommentsByContentId(@Param("contentId") UUID contentId, Pageable pageable);
}
