package com.project.content_service.service;

import com.project.content_service.domain.dto.response.ContentDetailResponse;
import com.project.content_service.domain.dto.response.ContentMediaResponse;
import com.project.content_service.domain.entity.ContentMedia;
import com.project.content_service.domain.enums.RedisKey;
import com.project.content_service.domain.mapper.ContentMapper;
import com.project.content_service.exception.customException.NoFollowingException;
import com.project.content_service.repository.CommentsRepository;
import com.project.content_service.repository.ContentRepository;
import com.project.content_service.repository.LikeShareRepository;
import com.project.content_service.repository.ShareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class Recommendation {
    private final ContentRepository contentRepository;
    private final ContentMapper contentMapper;
    private final CommentsRepository commentsRepository;
    private final LikeShareRepository likeShareRepository;
    private final ShareRepository shareRepository;
    private final RedisService redisService;
    private final GrpcServices grpcServices;

    private static final long MAX_MEDIA_SIZE = 5L * 1024 * 1024;
    private static final int PAGE_SIZE = 12;
    private static final int MAX_IDS_LIMIT = 10;
    private static final int COMMENTS_PAGE_SIZE = 20;
    private static final long REDIS_TTL = 600L;

    public Page<ContentDetailResponse> getContentOfUserFollowing(UUID id, int pageNumber) {
        log.trace("The user get content of user following by id ; {}" , id);
        return getContentByUserIds(grpcServices.getUserFollowing(id), id, pageNumber);
    }

    public Page<ContentDetailResponse> getContentOfGroupMembers(UUID groupId, UUID currentUserId, int pageNumber) {
        return getContentByUserIds(grpcServices.getUserGroupMembersByGroupId(groupId), currentUserId, pageNumber);
    }

    public Page<ContentDetailResponse> getContentOfAllGroupsMembers(UUID userId, int pageNumber) {
        return getContentByUserIds(grpcServices.getUserAllGroupsMembersByUserId(userId), userId, pageNumber);
    }

    private Page<ContentDetailResponse> getContentByUserIds(List<UUID> ids, UUID currentUserId, int pageNumber) {
        if (ids == null || ids.isEmpty()) {
            throw new NoFollowingException("No user/group members found");
        }

        System.out.println(Arrays.toString(ids.toArray()));

        if (pageNumber < 0) {
            pageNumber = 0;
        }
        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
        Page<ContentDetailResponse> responsePage = contentRepository.getContentDetailsByUserIds(ids, currentUserId,
                pageable);

        log.trace("Content is got with number : {}" , responsePage.getSize());
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
        log.trace("Redis is done");
        List<UUID> responseIds = responsePage.getContent().stream()
                .map(ContentDetailResponse::getId)
                .collect(Collectors.toList());
        List<ContentMedia> mediaList = contentRepository.getMediaByIds(responseIds);
        Map<UUID, ContentMedia> mediaMap = mediaList.stream()
                .collect(Collectors.toMap(ContentMedia::getId, m -> m));
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
        log.info("Found {} contents for provided IDs", responsePage.getNumberOfElements());
        return responsePage;
    }
}
