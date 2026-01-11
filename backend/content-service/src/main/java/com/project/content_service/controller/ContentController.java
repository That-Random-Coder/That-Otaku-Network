package com.project.content_service.controller;

import com.project.content_service.domain.dto.request.AddCommentRequest;
import com.project.content_service.domain.dto.request.CreateContentRequest;
import com.project.content_service.domain.dto.request.LikeDislikeRequest;
import com.project.content_service.domain.dto.response.CommentResponse;
import com.project.content_service.domain.dto.response.ContentDetailResponse;
import com.project.content_service.domain.dto.response.ContentResponse;
import com.project.content_service.domain.dto.response.InteractionDto;
import com.project.content_service.service.ContentServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/content")
public class ContentController {
    private static final Logger log = LoggerFactory.getLogger(ContentController.class);

    private final ContentServiceImpl contentService;
    private final com.project.content_service.service.Recommendation recommendationService;

    @GetMapping("/recommendation/following")
    public ResponseEntity<Page<ContentDetailResponse>> getContentOfUserFollowing(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(recommendationService.getContentOfUserFollowing(userId, page));
    }

    @GetMapping("/recommendation/group-members")
    public ResponseEntity<Page<ContentDetailResponse>> getContentOfGroupMembers(
            @RequestParam UUID groupId,
            @RequestParam UUID currentUserId,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(recommendationService.getContentOfGroupMembers(groupId, currentUserId, page));
    }

    @GetMapping("/recommendation/all-groups-members")
    public ResponseEntity<Page<ContentDetailResponse>> getContentOfAllGroupsMembers(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(recommendationService.getContentOfAllGroupsMembers(userId, page));
    }

    private UUID getAuthenticatedUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            return UUID.fromString(userDetails.getUsername());
        } else if (principal instanceof String str) {
            return UUID.fromString(str);
        }
        throw new IllegalArgumentException("Cannot extract user id from authentication principal");
    }

    @PreAuthorize("authentication.principal.id.equals(#request.userID)")
    @PostMapping("/create")
    public ResponseEntity<ContentResponse> createContent(
            @Valid @RequestPart CreateContentRequest request,
            @RequestPart MultipartFile media) {
        ContentResponse response = contentService.createContent(request, media);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/get")
    public ResponseEntity<ContentDetailResponse> getContentById(
            @RequestParam UUID contentId,
            @RequestParam UUID currentUserId,
            @RequestParam(defaultValue = "true") boolean includeMedia) {
        ContentDetailResponse response = contentService.getContentDetailById(contentId, currentUserId, includeMedia);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get/batch")
    public ResponseEntity<Page<ContentDetailResponse>> getContentsByIds(
            @RequestParam List<UUID> contentIds,
            @RequestParam UUID currentUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "true") boolean includeMedia) {
        Page<ContentDetailResponse> responses = contentService.getContentsByIds(contentIds, currentUserId, page,
                includeMedia);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/user")
    public ResponseEntity<Page<ContentDetailResponse>> getContentsByUserId(
            @RequestParam UUID userId,
            @RequestParam UUID currentUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "true") boolean includeMedia) {
        Page<ContentDetailResponse> responses = contentService.getContentsByUserId(userId, currentUserId, page,
                includeMedia);
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasRole('MODERATOR') OR @contentServiceImpl.isOwner(authentication.principal.id, #contentId)")
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteContent(@RequestParam UUID contentId) {
        contentService.deleteContent(contentId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('MODERATOR') OR @contentServiceImpl.isOwner(authentication.principal.id, #contentId)")
    @PutMapping("/disable")
    public ResponseEntity<Void> disableContent(@RequestParam UUID contentId) {
        contentService.disableContent(contentId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('MODERATOR') OR @contentServiceImpl.isOwner(authentication.principal.id, #contentId)")
    @PutMapping("/enable")
    public ResponseEntity<Void> enableContent(@RequestParam UUID contentId) {
        contentService.enableContent(contentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/comments")
    public ResponseEntity<Page<CommentResponse>> getCommentsByContentId(
            @RequestParam UUID contentId,
            @RequestParam(defaultValue = "0") int page) {
        Page<CommentResponse> comments = contentService.getCommentsByContentId(contentId, page);
        return ResponseEntity.ok(comments);
    }

    @PreAuthorize("authentication.principal.id.equals(#request.userId)")
    @PostMapping("/comment/add")
    public ResponseEntity<Void> addComment(@Valid @RequestBody AddCommentRequest request) {
        contentService.addComment(request);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PreAuthorize("authentication.principal.id.equals(#request.userId)")
    @PostMapping("/like")
    public ResponseEntity<InteractionDto> likeContent(@Valid @RequestBody LikeDislikeRequest request) {
        return ResponseEntity.ok().body(contentService.likeContent(request));
    }

    @PreAuthorize("authentication.principal.id.equals(#request.userId)")
    @PostMapping("/dislike")
    public ResponseEntity<InteractionDto> dislikeContent(@Valid @RequestBody LikeDislikeRequest request) {
        return ResponseEntity.ok().body(contentService.dislikeContent(request));
    }

    @PreAuthorize("authentication.principal.id.equals(#userId)")
    @PostMapping("/share")
    public ResponseEntity<Void> shareContent(
            @RequestParam UUID contentId,
            @RequestParam UUID userId) {
        contentService.shareContent(contentId, userId);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
