package com.project.content_service.domain.mapper;

import com.project.content_service.domain.dto.request.CreateContentRequest;
import com.project.content_service.domain.dto.response.ContentDetailResponse;
import com.project.content_service.domain.dto.response.ContentResponse;
import com.project.content_service.domain.entity.Content;
import com.project.content_service.domain.entity.ContentMedia;
import org.springframework.stereotype.Component;

@Component
public class ContentMapper {

    public ContentResponse toResponse(Content content) {
        if (content == null) {
            return null;
        }

        return ContentResponse.builder()
                .id(content.getId())
                .title(content.getTitle())
                .animeCategories(content.getAnimeCategories())
                .genre(content.getGenre())
                .bio(content.getBio())
                .enable(content.getEnable())
                .created(content.getCreated())
                .build();
    }

    public ContentDetailResponse toDetailResponse(Content content, Long likeCount, Long dislikeCount,
            Long commentCount) {
        if (content == null) {
            return null;
        }

        ContentMedia media = content.getContentMedia();
        byte[] mediaBytes = null;
        String mediaType = null;

        if (media != null) {
            mediaBytes = media.getContent();
            mediaType = media.getContentType();
        }

        return ContentDetailResponse.builder()
                .id(content.getId())
                .title(content.getTitle())
                .bio(content.getBio())
                .userID(content.getUserID())
                .userName(content.getUserName())
                .displayName(content.getDisplayName())
                .media(mediaBytes)
                .mediaType(mediaType)
                .likeCount(likeCount != null ? likeCount : 0L)
                .dislikeCount(dislikeCount != null ? dislikeCount : 0L)
                .commentCount(commentCount != null ? commentCount : 0L)
                .created(content.getCreated())
                .build();
    }

    public Content toEntity(CreateContentRequest request) {
        if (request == null) {
            return null;
        }

        return Content.builder()
                .title(request.getTitle())
                .animeCategories(request.getAnimeCategories())
                .genre(request.getGenres())
                .bio(request.getBio())
                .build();
    }
}
