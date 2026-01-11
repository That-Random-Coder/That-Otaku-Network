package com.project.content_service.service;

import com.project.content_service.domain.dto.response.*;
import com.project.content_service.domain.entity.Content;
import com.project.content_service.domain.entity.Comments;
import com.project.content_service.domain.entity.ContentMedia;
import com.project.content_service.domain.entity.LikeShare;
import com.project.content_service.domain.entity.Share;
import com.project.content_service.domain.enums.KafkaDomain;
import com.project.content_service.domain.enums.KafkaType;
import com.project.content_service.domain.enums.LikeOrDislikeEnums;
import com.project.content_service.domain.enums.RedisKey;
import com.project.content_service.domain.mapper.ContentMapper;
import com.project.content_service.repository.ContentRepository;
import com.project.content_service.repository.LikeShareRepository;
import com.project.content_service.repository.ShareRepository;
import com.project.content_service.domain.dto.request.AddCommentRequest;
import com.project.content_service.domain.dto.request.CreateContentRequest;
import com.project.content_service.domain.dto.request.LikeDislikeRequest;
import com.project.content_service.exception.customException.ContentNotFoundException;
import com.project.content_service.exception.customException.ImageUploadFailedException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.project.content_service.repository.CommentsRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional

public class ContentServiceImpl {

    private final ContentRepository contentRepository;
    private final ContentMapper contentMapper;
    private final CommentsRepository commentsRepository;
    private final LikeShareRepository likeShareRepository;
    private final ShareRepository shareRepository;
    private final RedisService redisService;
    private final KafkaServices kafkaService;
    private final EntityManager entityManager;

    private static final long MAX_MEDIA_SIZE = 5L * 1024 * 1024;
    private static final int PAGE_SIZE = 12;
    private static final int MAX_IDS_LIMIT = 10;
    private static final int COMMENTS_PAGE_SIZE = 20;
    private static final long REDIS_TTL = 600L;
    private static final Set<String> ALLOWED_MEDIA_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif");

    public boolean isOwner(UUID userId, UUID contentId) {
        return contentRepository.existsByIdAndUserID(contentId, userId);
    }

    private void validateContentMedia(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Content media is required");
        }

        if (file.getSize() > MAX_MEDIA_SIZE) {
            throw new IllegalArgumentException("Content media must be less than 50 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MEDIA_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG, WEBP, GIF images or MP4, WEBM videos are allowed");
        }
    }

    @Transactional
    public ContentResponse createContent(CreateContentRequest request, MultipartFile file) {
        log.info("Creating new content with title: {} by user: {}", request.getTitle(), request.getUserName());

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        if (request.getUserID() == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        byte[] mediaBytes = null;
        String mediaType = null;

        if (file != null && !file.isEmpty()) {
            validateContentMedia(file);

            try {
                mediaBytes = file.getBytes();
                mediaType = file.getContentType();
            } catch (IOException e) {
                log.error("Failed to upload content media for user: {}", request.getUserID());
                throw new ImageUploadFailedException("Failed to upload content media");
            }
        } else {
            throw new IllegalArgumentException("No Content");
        }

        if (request.getTags().size() > 10) {
            throw new IllegalArgumentException("Tag is more than 10");
        }

        Content content = Content.builder()
                .title(request.getTitle())
                .animeCategories(request.getAnimeCategories())
                .genre(request.getGenres())
                .tags(request.getTags())
                .bio(request.getBio())
                .userID(request.getUserID())
                .userName(request.getUserName())
                .displayName(request.getDisplayName())
                .enable(true)
                .created(LocalDateTime.now())
                .build();

        contentRepository.save(content);
        ContentMedia contentMedia = ContentMedia.builder()
                .id(content.getId())
                .content(mediaBytes)
                .contentType(mediaType)
                .build();

        content.setContentMedia(contentMedia);

        log.info("Content created successfully with ID: {} by user: {}", content.getId(), request.getUserID());

        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put(KafkaDomain.CONTENT_ID.toString(), content.getId().toString());
        objectMap.put(KafkaDomain.CONTENT_TITLE.name(), content.getTitle());
        objectMap.put(KafkaDomain.USERNAME.name(), content.getUserName());
        objectMap.put(KafkaDomain.TIME_OF_CREATION.name(), LocalDateTime.now());
        objectMap.put(KafkaDomain.CONTENT_CATEGORY.name(), content.getAnimeCategories());
        objectMap.put(KafkaDomain.CONTENT_GENRE.name(), content.getGenre());
        objectMap.put(KafkaDomain.CONTENT_TAG.name(), content.getTags());

        try {
            entityManager.flush();
            kafkaService.sendContentEvent(KafkaType.CREATE, objectMap);
        } catch (Exception e) {
            log.error("Failed to Kafka event in Create Message due to {}" , e.getMessage());
        }

        return contentMapper.toResponse(content);
    }

    @Transactional
    public void deleteContent(UUID contentId) {

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException("Content not found"));

        contentRepository.delete(content);

        entityManager.flush();

        redisService.delete(RedisKey.CONTENT_ + contentId.toString());
        redisService.delete(RedisKey.CONTENT_MEDIA_ + contentId.toString());

        kafkaService.sendContentEvent(
                KafkaType.DELETE,
                Map.of(KafkaDomain.CONTENT_ID.toString(), contentId.toString())
        );

        log.info("Content deleted successfully with ID: {}", contentId);
    }


    @Transactional
    public void disableContent(UUID contentId) {
        log.info("Disabling content with ID: {}", contentId);

        if (contentId == null) {
            throw new IllegalArgumentException("Content ID is required");
        }

        int updated = contentRepository.disableByContentId(contentId);
        if (updated == 0) {
            log.error("Content with ID: {} not found", contentId);
            throw new ContentNotFoundException("Content with ID: " + contentId + " not found");
        }

        redisService.delete(RedisKey.CONTENT_ + contentId.toString());
        log.info("Content disabled successfully with ID: {}", contentId);

        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put(KafkaDomain.CONTENT_ID.toString(), contentId.toString());
        try {
            entityManager.flush();
            kafkaService.sendContentEvent(KafkaType.DISABLE, objectMap);
        } catch (Exception e) {
            log.error("Failed to Kafka event in DISABLE Message due to {}" , e.getMessage());
        }
    }

    @Transactional
    public void enableContent(UUID contentId) {
        log.info("Enabling content with ID: {}", contentId);

        if (contentId == null) {
            throw new IllegalArgumentException("Content ID is required");
        }

        int updated = contentRepository.enableByContentId(contentId);
        if (updated == 0) {
            log.error("Content with ID: {} not found", contentId);
            throw new ContentNotFoundException("Content with ID: " + contentId + " not found");
        }

        redisService.delete(RedisKey.CONTENT_ + contentId.toString());
        log.info("Content enabled successfully with ID: {}", contentId);

        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put(KafkaDomain.CONTENT_ID.toString(), contentId.toString());

        try {
            entityManager.flush();
            kafkaService.sendContentEvent(KafkaType.ENABLE, objectMap);
        } catch (Exception e) {
            log.error("Failed to Kafka event in Enable Message due to {}" , e.getMessage());
        }
    }

    public ContentDetailResponse getContentDetailById(UUID contentId, UUID userId, boolean includeMedia) {
        log.info("Fetching content detail with ID: {}, includeMedia: {}", contentId, includeMedia);

        if (contentId == null) {
            throw new IllegalArgumentException("Content ID is required");
        }

        String cacheKey = RedisKey.CONTENT_ + contentId.toString();
        String mediaCacheKey = RedisKey.CONTENT_MEDIA_ + contentId.toString();

        ContentDetailResponse cached = redisService.get(cacheKey, ContentDetailResponse.class);
        ContentMediaResponse mediaResponse = redisService.get(mediaCacheKey, ContentMediaResponse.class);

        if (cached != null && mediaResponse != null) {
            redisService.set(cacheKey, cached, REDIS_TTL);
            redisService.set(mediaCacheKey, mediaResponse, REDIS_TTL);

            if (userId != null) {
                likeShareRepository.findByContentIdAndUserId(contentId, userId).ifPresentOrElse(
                        likeShare -> {
                            cached.setIsLiked(likeShare.getLikeOrDislike() == LikeOrDislikeEnums.LIKE);
                            cached.setIsDisliked(likeShare.getLikeOrDislike() == LikeOrDislikeEnums.DISLIKE);
                        },
                        () -> {
                            cached.setIsLiked(false);
                            cached.setIsDisliked(false);
                        });
            } else {
                cached.setIsLiked(false);
                cached.setIsDisliked(false);
            }

            if (includeMedia) {
                cached.setMedia(mediaResponse.getMedia());
                cached.setMediaType(mediaResponse.getMediaType());
            }

            return cached;
        }

        ContentDetailResponse response = contentRepository.getContentDetailById(contentId, userId)
                .orElseThrow(() -> {
                    log.error("Content with ID: {} not found", contentId);
                    return new ContentNotFoundException("Content with ID: " + contentId + " not found");
                });

        ContentDetailResponse cacheResponse = ContentDetailResponse.builder()
                .id(response.getId())
                .title(response.getTitle())
                .bio(response.getBio())
                .userID(response.getUserID())
                .userName(response.getUserName())
                .displayName(response.getDisplayName())
                .likeCount(response.getLikeCount())
                .dislikeCount(response.getDislikeCount())
                .commentCount(response.getCommentCount())
                .shareCount(response.getShareCount())
                .isLiked(response.getIsLiked())
                .isDisliked(response.getIsDisliked())
                .created(response.getCreated())
                .build();
        redisService.set(cacheKey, cacheResponse, REDIS_TTL);

        if (includeMedia) {
            loadMediaFromCacheOrDb(contentId, response);
        }

        log.info("Content detail fetched successfully with ID: {}", contentId);
        return response;
    }

    private void loadMediaFromCacheOrDb(UUID contentId, ContentDetailResponse response) {
        String mediaCacheKey = RedisKey.CONTENT_MEDIA_ + contentId.toString();
        ContentMediaResponse cachedMedia = redisService.get(mediaCacheKey, ContentMediaResponse.class);

        if (cachedMedia != null) {
            response.setMedia(cachedMedia.getMedia());
            response.setMediaType(cachedMedia.getMediaType());
        } else {
            contentRepository.getMediaById(contentId).ifPresent(media -> {
                response.setMedia(media.getContent());
                response.setMediaType(media.getContentType());

                ContentMediaResponse mediaResponse = ContentMediaResponse.builder()
                        .id(contentId)
                        .media(media.getContent())
                        .mediaType(media.getContentType())
                        .build();
                redisService.set(mediaCacheKey, mediaResponse, REDIS_TTL);
            });
        }
    }

    public Page<ContentDetailResponse> getContentsByIds(List<UUID> contentIds, UUID userId, int page,
            boolean includeMedia) {
        log.info("Fetching contents for {} IDs, includeMedia: {}", contentIds != null ? contentIds.size() : 0,
                includeMedia);

        if (contentIds == null || contentIds.isEmpty()) {
            throw new IllegalArgumentException("Content IDs list is required");
        }

        if (contentIds.size() > MAX_IDS_LIMIT) {
            throw new IllegalArgumentException("Maximum " + MAX_IDS_LIMIT + " content IDs allowed per request");
        }

        if (page < 0) {
            page = 0;
        }

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<ContentDetailResponse> responsePage = contentRepository.getContentDetailsByIds(contentIds, userId,
                pageable);

        if (responsePage.isEmpty()) {
            log.info("No contents found for provided IDs");
            return responsePage;
        }

        responsePage.getContent().forEach(response -> {
            String cacheKey = RedisKey.CONTENT_ + response.getId().toString();
            ContentDetailResponse cacheResponse = ContentDetailResponse.builder()
                    .id(response.getId())
                    .title(response.getTitle())
                    .bio(response.getBio())
                    .userID(response.getUserID())
                    .userName(response.getUserName())
                    .displayName(response.getDisplayName())
                    .likeCount(response.getLikeCount())
                    .dislikeCount(response.getDislikeCount())
                    .commentCount(response.getCommentCount())
                    .shareCount(response.getShareCount())
                    .isLiked(response.getIsLiked())
                    .isDisliked(response.getIsDisliked())
                    .created(response.getCreated())
                    .build();
            redisService.set(cacheKey, cacheResponse, REDIS_TTL);
        });

        if (includeMedia) {
            List<UUID> ids = responsePage.getContent().stream()
                    .map(ContentDetailResponse::getId)
                    .collect(Collectors.toList());

            List<ContentMedia> mediaList = contentRepository.getMediaByIds(ids);
            Map<UUID, ContentMedia> mediaMap = mediaList.stream()
                    .collect(Collectors.toMap(
                            ContentMedia::getId,
                            m -> m));

            responsePage.getContent().forEach(response -> {
                ContentMedia media = mediaMap.get(response.getId());
                if (media != null) {
                    response.setMedia(media.getContent());
                    response.setMediaType(media.getContentType());

                    String mediaCacheKey = RedisKey.CONTENT_MEDIA_ + response.getId().toString();
                    ContentMediaResponse mediaResponse = ContentMediaResponse.builder()
                            .id(response.getId())
                            .media(media.getContent())
                            .mediaType(media.getContentType())
                            .build();
                    redisService.set(mediaCacheKey, mediaResponse, REDIS_TTL);
                }
            });
        }

        log.info("Found {} contents for provided IDs", responsePage.getNumberOfElements());
        return responsePage;
    }

    public Page<ContentDetailResponse> getContentsByUserId(UUID userId, UUID currentUserId, int page,
            boolean includeMedia) {
        log.info("Fetching contents for user ID: {}, page: {}, includeMedia: {}", userId, page, includeMedia);

        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        if (page < 0) {
            page = 0;
        }

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);

        Page<ContentDetailResponse> responsePage = contentRepository.getContentDetailsByUserId(userId, currentUserId,
                pageable);

        if (responsePage.isEmpty()) {
            log.info("No contents found for user ID: {}", userId);
            return responsePage;
        }

        responsePage.getContent().forEach(response -> {
            String cacheKey = RedisKey.CONTENT_ + response.getId().toString();
            ContentDetailResponse cacheResponse = ContentDetailResponse.builder()
                    .id(response.getId())
                    .title(response.getTitle())
                    .bio(response.getBio())
                    .userID(response.getUserID())
                    .userName(response.getUserName())
                    .displayName(response.getDisplayName())
                    .likeCount(response.getLikeCount())
                    .dislikeCount(response.getDislikeCount())
                    .commentCount(response.getCommentCount())
                    .shareCount(response.getShareCount())
                    .isLiked(response.getIsLiked())
                    .isDisliked(response.getIsDisliked())
                    .created(response.getCreated())
                    .build();
            redisService.set(cacheKey, cacheResponse, REDIS_TTL);
        });

        if (includeMedia) {
            List<UUID> contentIds = responsePage.getContent().stream()
                    .map(ContentDetailResponse::getId)
                    .collect(Collectors.toList());

            List<ContentMedia> mediaList = contentRepository.getMediaByIds(contentIds);
            Map<UUID, ContentMedia> mediaMap = mediaList.stream()
                    .collect(Collectors.toMap(
                            ContentMedia::getId,
                            m -> m));

            responsePage.getContent().forEach(response -> {
                ContentMedia media = mediaMap.get(response.getId());
                if (media != null) {
                    response.setMedia(media.getContent());
                    response.setMediaType(media.getContentType());

                    String mediaCacheKey = RedisKey.CONTENT_MEDIA_ + response.getId().toString();
                    ContentMediaResponse mediaResponse = ContentMediaResponse.builder()
                            .id(response.getId())
                            .media(media.getContent())
                            .mediaType(media.getContentType())
                            .build();
                    redisService.set(mediaCacheKey, mediaResponse, REDIS_TTL);
                }
            });
        }

        log.info("Found {} contents for user ID: {}", responsePage.getNumberOfElements(), userId);
        return responsePage;
    }

    public Page<CommentResponse> getCommentsByContentId(UUID contentId, int page) {
        log.info("Fetching comments for content ID: {}, page: {}", contentId, page);

        if (contentId == null) {
            throw new IllegalArgumentException("Content ID is required");
        }

        if (page < 0) {
            page = 0;
        }

        Pageable pageable = PageRequest.of(page, COMMENTS_PAGE_SIZE);
        Page<CommentResponse> comments = commentsRepository.getCommentsByContentId(contentId, pageable);

        log.info("Found {} comments for content ID: {}", comments.getNumberOfElements(), contentId);
        return comments;
    }

    @Transactional
    public void addComment(AddCommentRequest request) {
        log.info("Adding comment for content ID: {} by user: {}", request.getContentId(), request.getUserId());

        if (request.getContentId() == null) {
            throw new IllegalArgumentException("Content ID is required");
        }

        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        if (request.getComment() == null || request.getComment().isBlank()) {
            throw new IllegalArgumentException("Comment is required");
        }

        Content content = contentRepository.findByIdAndEnableTrue(request.getContentId())
                .orElseThrow(() -> new ContentNotFoundException(
                        "Content with ID: " + request.getContentId() + " not found"));

        Comments comment = Comments.builder()
                .content(content)
                .comment(request.getComment())
                .userId(request.getUserId())
                .userUserName(request.getUserName())
                .commentAt(LocalDateTime.now())
                .build();

        commentsRepository.save(comment);

        String cacheKey = RedisKey.CONTENT_ + request.getContentId().toString();
        ContentDetailResponse cached = redisService.get(cacheKey, ContentDetailResponse.class);
        if (cached != null) {
            cached.setCommentCount(cached.getCommentCount() + 1);
            redisService.set(cacheKey, cached, REDIS_TTL);
        }
        log.info("Comment added successfully for content ID: {}", request.getContentId());

        Map<String, Object> map = new HashMap<>();
        map.put(KafkaDomain.CONTENT_ID.toString(), request.getContentId().toString());
        map.put(KafkaDomain.USER_ID.toString(), request.getUserId().toString());
        map.put(KafkaDomain.TIME_OF_CREATION.toString(), LocalDateTime.now());

        try {
            entityManager.flush();
            kafkaService.sendContentEvent(KafkaType.COMMENT, map);
        } catch (Exception e) {
            log.error("Failed to Kafka event in Comment Message due to {}" , e.getMessage());
        }

    }

    @Transactional
    public InteractionDto likeContent(LikeDislikeRequest request) {
        log.info("Liking content ID: {} by user: {}", request.getContentId(), request.getUserId());

        if (request.getContentId() == null) {
            throw new IllegalArgumentException("Content ID is required");
        }

        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        Content content = contentRepository.findByIdAndEnableTrue(request.getContentId())
                .orElseThrow(() -> new ContentNotFoundException(
                        "Content with ID: " + request.getContentId() + " not found"));

        LikeShare existing = likeShareRepository.findByContentIdAndUserId(request.getContentId(), request.getUserId())
                .orElse(null);

        String cacheKey = RedisKey.CONTENT_ + request.getContentId().toString();
        ContentDetailResponse cached = redisService.get(cacheKey, ContentDetailResponse.class);

        Map<String, Object> map = new HashMap<>();

        map.put(KafkaDomain.CONTENT_ID.toString(), request.getContentId().toString());
        map.put(KafkaDomain.USER_ID.toString(), request.getUserId().toString());
        map.put(KafkaDomain.TIME_OF_CREATION.toString(), LocalDateTime.now());
        KafkaType type = null;

        String response = "Failed";
        if (existing != null) {
            LikeOrDislikeEnums previousStatus = existing.getLikeOrDislike();
            if (previousStatus == LikeOrDislikeEnums.LIKE) {
                likeShareRepository.delete(existing);
                type = KafkaType.REMOVE_LIKE;
                response = "Remove Liked";
            } else {
                existing.setLikeOrDislike(LikeOrDislikeEnums.LIKE);
                existing.setLikeAndDislikeAt(LocalDateTime.now());
                likeShareRepository.save(existing);
                if (cached != null) {
                    cached.setLikeCount(cached.getLikeCount() + 1);
                    cached.setDislikeCount(Math.max(0, cached.getDislikeCount() - 1));
                    redisService.set(cacheKey, cached, REDIS_TTL);
                }
                type = KafkaType.CHANGE_TO_LIKE;
                response = "Dislike -> Like";
            }
        } else {
            LikeShare likeShare = LikeShare
                    .builder()
                    .content(content)
                    .userId(request.getUserId())
                    .likeOrDislike(LikeOrDislikeEnums.LIKE)
                    .likeAndDislikeAt(LocalDateTime.now())
                    .build();
            likeShareRepository.save(likeShare);
            if (cached != null) {
                cached.setLikeCount(cached.getLikeCount() + 1);
                redisService.set(cacheKey, cached, REDIS_TTL);
            }
            response = "Liked";
            type = KafkaType.LIKE;
        }


        try {
            entityManager.flush();
            kafkaService.sendContentEvent(type, map);
        } catch (Exception e) {
            log.error("Failed to Kafka event in Like Message due to {}" , e.getMessage());
        }
        log.info("Content {} successfully ID: {} by user: {}", response, request.getContentId(), request.getUserId());
        return InteractionDto.builder()
                .output(response).build();
    }

    @Transactional
    public InteractionDto dislikeContent(LikeDislikeRequest request) {
        log.info("Disliking content ID: {} by user: {}", request.getContentId(), request.getUserId());

        if (request.getContentId() == null) {
            throw new IllegalArgumentException("Content ID is required");
        }

        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        Content content = contentRepository.findByIdAndEnableTrue(request.getContentId())
                .orElseThrow(() -> new ContentNotFoundException(
                        "Content with ID: " + request.getContentId() + " not found"));

        LikeShare existing = likeShareRepository.findByContentIdAndUserId(request.getContentId(), request.getUserId())
                .orElse(null);

        String cacheKey = RedisKey.CONTENT_ + request.getContentId().toString();
        ContentDetailResponse cached = redisService.get(cacheKey, ContentDetailResponse.class);

        Map<String, Object> map = new HashMap<>();

        map.put(KafkaDomain.CONTENT_ID.toString(), request.getContentId().toString());
        map.put(KafkaDomain.USER_ID.toString(), request.getUserId().toString());
        map.put(KafkaDomain.TIME_OF_CREATION.toString(), LocalDateTime.now());
        KafkaType type = null;

        String response = "Failed";
        if (existing != null) {

            LikeOrDislikeEnums previousStatus = existing.getLikeOrDislike();
            if (previousStatus == LikeOrDislikeEnums.DISLIKE) {
                likeShareRepository.delete(existing);
                type = KafkaType.REMOVE_DISLIKE;
                response = "Remove Disliked";
            } else {
                existing.setLikeOrDislike(LikeOrDislikeEnums.DISLIKE);
                existing.setLikeAndDislikeAt(LocalDateTime.now());
                likeShareRepository.save(existing);
                if (cached != null) {
                    cached.setDislikeCount(cached.getDislikeCount() + 1);
                    cached.setLikeCount(Math.max(0, cached.getLikeCount() - 1));
                    redisService.set(cacheKey, cached, REDIS_TTL);
                }
                type = KafkaType.CHANGE_TO_DISLIKE;
                response = "Like -> Dislike";
            }
        } else {
            LikeShare likeShare = LikeShare.builder()
                    .content(content)
                    .userId(request.getUserId())
                    .likeOrDislike(LikeOrDislikeEnums.DISLIKE)
                    .likeAndDislikeAt(LocalDateTime.now())
                    .build();
            likeShareRepository.save(likeShare);
            if (cached != null) {
                cached.setDislikeCount(cached.getDislikeCount() + 1);
                redisService.set(cacheKey, cached, REDIS_TTL);
            }
            response = "DisLiked";
            type = KafkaType.DISLIKE;
        }
        try {
            entityManager.flush();
            kafkaService.sendContentEvent(type, map);
        } catch (Exception e) {
            log.error("Failed to Kafka event in DisLike Message due to {}" , e.getMessage());
        }
        log.info("Content {} successfully ID: {} by user: {}", response, request.getContentId(), request.getUserId());
        return InteractionDto.builder()
                .output(response).build();
    }

    @Transactional
    public void shareContent(UUID contentId, UUID userId) {
        log.info("Sharing content ID: {} by user: {}", contentId, userId);

        if (contentId == null) {
            throw new IllegalArgumentException("Content ID is required");
        }

        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        Content content = contentRepository.findByIdAndEnableTrue(contentId)
                .orElseThrow(() -> new ContentNotFoundException(
                        "Content with ID: " + contentId + " not found"));

        Share share = Share.builder()
                .content(content)
                .userId(userId)
                .sharedAt(LocalDateTime.now())
                .build();

        shareRepository.save(share);

        String cacheKey = RedisKey.CONTENT_ + contentId.toString();
        ContentDetailResponse cached = redisService.get(cacheKey, ContentDetailResponse.class);
        if (cached != null) {
            cached.setShareCount(cached.getShareCount() + 1);
            redisService.set(cacheKey, cached, REDIS_TTL);
        }

        Map<String, Object> map = new HashMap<>();
        map.put(KafkaDomain.CONTENT_ID.toString(), contentId.toString());
        map.put(KafkaDomain.USER_ID.toString(), userId.toString());
        map.put(KafkaDomain.TIME_OF_CREATION.toString(), LocalDateTime.now());
        try {
            entityManager.flush();
            kafkaService.sendContentEvent(KafkaType.SHARE, map);
        } catch (Exception e) {
            log.error("Failed to Kafka event in DisLike Message due to {}" , e.getMessage());
        }
        log.info("Content shared successfully ID: {} by user: {}", contentId, userId);
    }
}
