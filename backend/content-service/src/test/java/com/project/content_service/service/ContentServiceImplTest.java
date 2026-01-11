package com.project.content_service.service;

import com.project.content_service.domain.dto.request.AddCommentRequest;
import com.project.content_service.domain.dto.request.CreateContentRequest;
import com.project.content_service.domain.dto.request.LikeDislikeRequest;
import com.project.content_service.domain.dto.response.CommentResponse;
import com.project.content_service.domain.dto.response.ContentDetailResponse;
import com.project.content_service.domain.dto.response.ContentMediaResponse;
import com.project.content_service.domain.dto.response.ContentResponse;
import com.project.content_service.domain.entity.Content;
import com.project.content_service.domain.entity.ContentMedia;
import com.project.content_service.domain.entity.LikeShare;
import com.project.content_service.domain.enums.Category;
import com.project.content_service.domain.enums.Genre;
import com.project.content_service.domain.enums.LikeOrDislikeEnums;
import com.project.content_service.domain.enums.RedisKey;
import com.project.content_service.domain.mapper.ContentMapper;
import com.project.content_service.exception.customException.ContentNotFoundException;
import com.project.content_service.exception.customException.ImageUploadFailedException;
import com.project.content_service.repository.CommentsRepository;
import com.project.content_service.repository.ContentRepository;
import com.project.content_service.repository.LikeShareRepository;
import com.project.content_service.repository.ShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentServiceImplTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentMapper contentMapper;

    @Mock
    private CommentsRepository commentsRepository;

    @Mock
    private LikeShareRepository likeShareRepository;

    @Mock
    private ShareRepository shareRepository;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private ContentServiceImpl contentService;

    private UUID contentId;
    private UUID userId;
    private Content testContent;
    private ContentDetailResponse testDetailResponse;
    private CreateContentRequest createRequest;
    private MockMultipartFile validMediaFile;

    @BeforeEach
    void setUp() {
        contentId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testContent = Content.builder()
                .id(contentId)
                .title("Test Content")
                .bio("Test Bio")
                .userID(userId)
                .userName("testuser")
                .displayName("Test User")
                .animeCategories(Set.of(Category.MEME))
                .genre(Set.of(Genre.ACTION))
                .tags(Set.of("tag1", "tag2"))
                .enable(true)
                .created(LocalDateTime.now())
                .build();

        testDetailResponse = ContentDetailResponse.builder()
                .id(contentId)
                .title("Test Content")
                .bio("Test Bio")
                .userID(userId)
                .userName("testuser")
                .displayName("Test User")
                .likeCount(10L)
                .dislikeCount(2L)
                .commentCount(5L)
                .shareCount(3L)
                .isLiked(false)
                .isDisliked(false)
                .created(LocalDateTime.now())
                .build();

        createRequest = CreateContentRequest.builder()
                .title("New Content")
                .animeCategories(Set.of(Category.MEME))
                .genres(Set.of(Genre.ACTION))
                .tags(Set.of("tag1"))
                .bio("New Bio")
                .userID(userId)
                .userName("testuser")
                .displayName("Test User")
                .build();

        validMediaFile = new MockMultipartFile(
                "media",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes());
    }

    @Nested
    @DisplayName("isOwner Tests")
    class IsOwnerTests {

        @Test
        @DisplayName("Should return true when user owns the content")
        void isOwner_WhenUserOwnsContent_ReturnsTrue() {
            when(contentRepository.existsByIdAndUserID(contentId, userId)).thenReturn(true);

            boolean result = contentService.isOwner(userId, contentId);

            assertThat(result).isTrue();
            verify(contentRepository).existsByIdAndUserID(contentId, userId);
        }

        @Test
        @DisplayName("Should return false when user does not own the content")
        void isOwner_WhenUserDoesNotOwnContent_ReturnsFalse() {
            when(contentRepository.existsByIdAndUserID(contentId, userId)).thenReturn(false);

            boolean result = contentService.isOwner(userId, contentId);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("createContent Tests")
    class CreateContentTests {

        @Test
        @DisplayName("Should create content successfully with valid request")
        void createContent_WithValidRequest_ReturnsContentResponse() {
            ContentResponse expectedResponse = ContentResponse.builder()
                    .id(contentId)
                    .title("New Content")
                    .build();

            when(contentMapper.toResponse(any(Content.class))).thenReturn(expectedResponse);

            ContentResponse result = contentService.createContent(createRequest, validMediaFile);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(contentId);
            verify(contentRepository).save(any(Content.class));
        }

        @Test
        @DisplayName("Should throw exception when title is null")
        void createContent_WithNullTitle_ThrowsException() {
            createRequest.setTitle(null);

            assertThatThrownBy(() -> contentService.createContent(createRequest, validMediaFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Title is required");
        }

        @Test
        @DisplayName("Should throw exception when title is blank")
        void createContent_WithBlankTitle_ThrowsException() {
            createRequest.setTitle("   ");

            assertThatThrownBy(() -> contentService.createContent(createRequest, validMediaFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Title is required");
        }

        @Test
        @DisplayName("Should throw exception when user ID is null")
        void createContent_WithNullUserId_ThrowsException() {
            createRequest.setUserID(null);

            assertThatThrownBy(() -> contentService.createContent(createRequest, validMediaFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User ID is required");
        }

        @Test
        @DisplayName("Should throw exception when file is null")
        void createContent_WithNullFile_ThrowsException() {
            assertThatThrownBy(() -> contentService.createContent(createRequest, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("No Content");
        }

        @Test
        @DisplayName("Should throw exception when file is empty")
        void createContent_WithEmptyFile_ThrowsException() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "media", "test.jpg", "image/jpeg", new byte[0]);

            assertThatThrownBy(() -> contentService.createContent(createRequest, emptyFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("No Content");
        }

        @Test
        @DisplayName("Should throw exception when file exceeds max size")
        void createContent_WithOversizedFile_ThrowsException() {
            byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
            MockMultipartFile largeFile = new MockMultipartFile(
                    "media", "test.jpg", "image/jpeg", largeContent);

            assertThatThrownBy(() -> contentService.createContent(createRequest, largeFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Content media must be less than 50 MB");
        }

        @Test
        @DisplayName("Should throw exception when file type is invalid")
        void createContent_WithInvalidFileType_ThrowsException() {
            MockMultipartFile invalidFile = new MockMultipartFile(
                    "media", "test.pdf", "application/pdf", "content".getBytes());

            assertThatThrownBy(() -> contentService.createContent(createRequest, invalidFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only JPEG, PNG, WEBP, GIF images");
        }
    }

    @Nested
    @DisplayName("deleteContent Tests")
    class DeleteContentTests {

        @Test
        @DisplayName("Should delete content successfully")
        void deleteContent_WithValidId_DeletesContent() {
            when(contentRepository.deleteByContentId(contentId)).thenReturn(1);

            contentService.deleteContent(contentId);

            verify(contentRepository).deleteByContentId(contentId);
            verify(redisService).delete(RedisKey.CONTENT_ + contentId.toString());
            verify(redisService).delete(RedisKey.CONTENT_MEDIA_ + contentId.toString());
        }

        @Test
        @DisplayName("Should throw exception when content ID is null")
        void deleteContent_WithNullId_ThrowsException() {
            assertThatThrownBy(() -> contentService.deleteContent(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Content ID is required");
        }

        @Test
        @DisplayName("Should throw exception when content not found")
        void deleteContent_WhenContentNotFound_ThrowsException() {
            when(contentRepository.deleteByContentId(contentId)).thenReturn(0);

            assertThatThrownBy(() -> contentService.deleteContent(contentId))
                    .isInstanceOf(ContentNotFoundException.class)
                    .hasMessageContaining(contentId.toString());
        }
    }

    @Nested
    @DisplayName("disableContent Tests")
    class DisableContentTests {

        @Test
        @DisplayName("Should disable content successfully")
        void disableContent_WithValidId_DisablesContent() {
            when(contentRepository.disableByContentId(contentId)).thenReturn(1);

            contentService.disableContent(contentId);

            verify(contentRepository).disableByContentId(contentId);
            verify(redisService).delete(RedisKey.CONTENT_ + contentId.toString());
        }

        @Test
        @DisplayName("Should throw exception when content ID is null")
        void disableContent_WithNullId_ThrowsException() {
            assertThatThrownBy(() -> contentService.disableContent(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Content ID is required");
        }

        @Test
        @DisplayName("Should throw exception when content not found")
        void disableContent_WhenContentNotFound_ThrowsException() {
            when(contentRepository.disableByContentId(contentId)).thenReturn(0);

            assertThatThrownBy(() -> contentService.disableContent(contentId))
                    .isInstanceOf(ContentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("enableContent Tests")
    class EnableContentTests {

        @Test
        @DisplayName("Should enable content successfully")
        void enableContent_WithValidId_EnablesContent() {
            when(contentRepository.enableByContentId(contentId)).thenReturn(1);

            contentService.enableContent(contentId);

            verify(contentRepository).enableByContentId(contentId);
            verify(redisService).delete(RedisKey.CONTENT_ + contentId.toString());
        }

        @Test
        @DisplayName("Should throw exception when content not found")
        void enableContent_WhenContentNotFound_ThrowsException() {
            when(contentRepository.enableByContentId(contentId)).thenReturn(0);

            assertThatThrownBy(() -> contentService.enableContent(contentId))
                    .isInstanceOf(ContentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getContentDetailById Tests")
    class GetContentDetailByIdTests {

        @Test
        @DisplayName("Should return cached content when available")
        void getContentDetailById_WhenCached_ReturnsCachedContent() {
            String cacheKey = RedisKey.CONTENT_ + contentId.toString();
            when(redisService.get(cacheKey, ContentDetailResponse.class)).thenReturn(testDetailResponse);

            ContentDetailResponse result = contentService.getContentDetailById(contentId, null, false);

            assertThat(result).isEqualTo(testDetailResponse);
            verify(contentRepository, never()).getContentDetailById(any(), any());
        }

        @Test
        @DisplayName("Should fetch from DB when not cached")
        void getContentDetailById_WhenNotCached_FetchesFromDb() {
            when(redisService.get(anyString(), eq(ContentDetailResponse.class))).thenReturn(null);
            when(contentRepository.getContentDetailById(contentId, null)).thenReturn(Optional.of(testDetailResponse));

            ContentDetailResponse result = contentService.getContentDetailById(contentId, null, false);

            assertThat(result).isNotNull();
            verify(contentRepository).getContentDetailById(contentId, null);
            verify(redisService).set(anyString(), eq(testDetailResponse), anyLong());
        }

        @Test
        @DisplayName("Should load media when includeMedia is true")
        void getContentDetailById_WithIncludeMedia_LoadsMedia() {
            when(redisService.get(anyString(), eq(ContentDetailResponse.class))).thenReturn(null);
            when(contentRepository.getContentDetailById(contentId, null)).thenReturn(Optional.of(testDetailResponse));
            when(redisService.get(anyString(), eq(ContentMediaResponse.class))).thenReturn(null);
            ContentMedia mediaData = ContentMedia.builder()
                    .id(contentId)
                    .content("test".getBytes())
                    .contentType("image/jpeg")
                    .build();
            when(contentRepository.getMediaById(contentId)).thenReturn(Optional.of(mediaData));

            ContentDetailResponse result = contentService.getContentDetailById(contentId, null, true);

            assertThat(result).isNotNull();
            verify(contentRepository).getMediaById(contentId);
        }

        @Test
        @DisplayName("Should throw exception when content ID is null")
        void getContentDetailById_WithNullId_ThrowsException() {
            assertThatThrownBy(() -> contentService.getContentDetailById(null, null, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Content ID is required");
        }

        @Test
        @DisplayName("Should throw exception when content not found")
        void getContentDetailById_WhenNotFound_ThrowsException() {
            when(redisService.get(anyString(), eq(ContentDetailResponse.class))).thenReturn(null);
            when(contentRepository.getContentDetailById(contentId, null)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contentService.getContentDetailById(contentId, null, false))
                    .isInstanceOf(ContentNotFoundException.class);
        }

        @Test
        @DisplayName("Should update like status from cache when user ID provided")
        void getContentDetailById_WithUserId_UpdatesLikeStatus() {
            String cacheKey = RedisKey.CONTENT_ + contentId.toString();
            when(redisService.get(cacheKey, ContentDetailResponse.class)).thenReturn(testDetailResponse);

            LikeShare likeShare = LikeShare.builder()
                    .likeOrDislike(LikeOrDislikeEnums.LIKE)
                    .build();
            when(likeShareRepository.findByContentIdAndUserId(contentId, userId))
                    .thenReturn(Optional.of(likeShare));

            ContentDetailResponse result = contentService.getContentDetailById(contentId, userId, false);

            assertThat(result.getIsLiked()).isTrue();
            assertThat(result.getIsDisliked()).isFalse();
        }
    }

    @Nested
    @DisplayName("getContentsByIds Tests")
    class GetContentsByIdsTests {

        @Test
        @DisplayName("Should return contents for valid IDs")
        void getContentsByIds_WithValidIds_ReturnsContents() {
            List<UUID> ids = List.of(contentId);
            Page<ContentDetailResponse> page = new PageImpl<>(List.of(testDetailResponse));
            when(contentRepository.getContentDetailsByIds(eq(ids), any(), any(Pageable.class))).thenReturn(page);

            Page<ContentDetailResponse> result = contentService.getContentsByIds(ids, null, 0, false);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw exception when IDs list is empty")
        void getContentsByIds_WithEmptyList_ThrowsException() {
            assertThatThrownBy(() -> contentService.getContentsByIds(List.of(), null, 0, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Content IDs list is required");
        }

        @Test
        @DisplayName("Should throw exception when IDs list exceeds limit")
        void getContentsByIds_WithTooManyIds_ThrowsException() {
            List<UUID> ids = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                ids.add(UUID.randomUUID());
            }

            assertThatThrownBy(() -> contentService.getContentsByIds(ids, null, 0, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Maximum");
        }

        @Test
        @DisplayName("Should load media when includeMedia is true")
        void getContentsByIds_WithIncludeMedia_LoadsMedia() {
            List<UUID> ids = List.of(contentId);
            Page<ContentDetailResponse> page = new PageImpl<>(List.of(testDetailResponse));
            when(contentRepository.getContentDetailsByIds(eq(ids), any(), any(Pageable.class))).thenReturn(page);

            List<ContentMedia> mediaList = new ArrayList<>();
            mediaList.add(ContentMedia.builder()
                    .id(contentId)
                    .content("test".getBytes())
                    .contentType("image/jpeg")
                    .build());
            when(contentRepository.getMediaByIds(anyList())).thenReturn(mediaList);

            Page<ContentDetailResponse> result = contentService.getContentsByIds(ids, null, 0, true);

            assertThat(result.getContent()).hasSize(1);
            verify(contentRepository).getMediaByIds(anyList());
        }
    }

    @Nested
    @DisplayName("getContentsByUserId Tests")
    class GetContentsByUserIdTests {

        @Test
        @DisplayName("Should return contents for valid user ID")
        void getContentsByUserId_WithValidId_ReturnsContents() {
            Page<ContentDetailResponse> page = new PageImpl<>(List.of(testDetailResponse));
            when(contentRepository.getContentDetailsByUserId(eq(userId), any(), any(Pageable.class))).thenReturn(page);

            Page<ContentDetailResponse> result = contentService.getContentsByUserId(userId, null, 0, false);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw exception when user ID is null")
        void getContentsByUserId_WithNullId_ThrowsException() {
            assertThatThrownBy(() -> contentService.getContentsByUserId(null, null, 0, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User ID is required");
        }
    }

    @Nested
    @DisplayName("getCommentsByContentId Tests")
    class GetCommentsByContentIdTests {

        @Test
        @DisplayName("Should return comments for valid content ID")
        void getCommentsByContentId_WithValidId_ReturnsComments() {
            CommentResponse comment = CommentResponse.builder()
                    .id(UUID.randomUUID())
                    .comment("Test comment")
                    .build();
            Page<CommentResponse> page = new PageImpl<>(List.of(comment));
            when(commentsRepository.getCommentsByContentId(eq(contentId), any(Pageable.class))).thenReturn(page);

            Page<CommentResponse> result = contentService.getCommentsByContentId(contentId, 0);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw exception when content ID is null")
        void getCommentsByContentId_WithNullId_ThrowsException() {
            assertThatThrownBy(() -> contentService.getCommentsByContentId(null, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Content ID is required");
        }
    }

    @Nested
    @DisplayName("addComment Tests")
    class AddCommentTests {

        @Test
        @DisplayName("Should add comment successfully")
        void addComment_WithValidRequest_AddsComment() {
            AddCommentRequest request = AddCommentRequest.builder()
                    .contentId(contentId)
                    .userId(userId)
                    .userName("testuser")
                    .comment("Test comment")
                    .build();

            when(contentRepository.findByIdAndEnableTrue(contentId)).thenReturn(Optional.of(testContent));
            when(redisService.get(anyString(), eq(ContentDetailResponse.class))).thenReturn(testDetailResponse);

            contentService.addComment(request);

            verify(commentsRepository).save(any());
            verify(redisService).set(anyString(), any(ContentDetailResponse.class), anyLong());
        }

        @Test
        @DisplayName("Should throw exception when content ID is null")
        void addComment_WithNullContentId_ThrowsException() {
            AddCommentRequest request = AddCommentRequest.builder()
                    .contentId(null)
                    .userId(userId)
                    .comment("Test")
                    .build();

            assertThatThrownBy(() -> contentService.addComment(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Content ID is required");
        }

        @Test
        @DisplayName("Should throw exception when user ID is null")
        void addComment_WithNullUserId_ThrowsException() {
            AddCommentRequest request = AddCommentRequest.builder()
                    .contentId(contentId)
                    .userId(null)
                    .comment("Test")
                    .build();

            assertThatThrownBy(() -> contentService.addComment(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User ID is required");
        }

        @Test
        @DisplayName("Should throw exception when comment is blank")
        void addComment_WithBlankComment_ThrowsException() {
            AddCommentRequest request = AddCommentRequest.builder()
                    .contentId(contentId)
                    .userId(userId)
                    .comment("   ")
                    .build();

            assertThatThrownBy(() -> contentService.addComment(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Comment is required");
        }

        @Test
        @DisplayName("Should throw exception when content not found")
        void addComment_WhenContentNotFound_ThrowsException() {
            AddCommentRequest request = AddCommentRequest.builder()
                    .contentId(contentId)
                    .userId(userId)
                    .userName("testuser")
                    .comment("Test comment")
                    .build();

            when(contentRepository.findByIdAndEnableTrue(contentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contentService.addComment(request))
                    .isInstanceOf(ContentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("likeContent Tests")
    class LikeContentTests {

        @Test
        @DisplayName("Should like content when no existing like/dislike")
        void likeContent_WhenNewLike_CreatesLike() {
            LikeDislikeRequest request = LikeDislikeRequest.builder()
                    .contentId(contentId)
                    .userId(userId)
                    .build();

            when(contentRepository.findByIdAndEnableTrue(contentId)).thenReturn(Optional.of(testContent));
            when(likeShareRepository.findByContentIdAndUserId(contentId, userId)).thenReturn(Optional.empty());
            when(redisService.get(anyString(), eq(ContentDetailResponse.class))).thenReturn(testDetailResponse);

            contentService.likeContent(request);

            verify(likeShareRepository).save(any(LikeShare.class));
            assertThat(testDetailResponse.getLikeCount()).isEqualTo(11L);
        }

        @Test
        @DisplayName("Should switch from dislike to like")
        void likeContent_WhenPreviouslyDisliked_SwitchesToLike() {
            LikeDislikeRequest request = LikeDislikeRequest.builder()
                    .contentId(contentId)
                    .userId(userId)
                    .build();

            LikeShare existingDislike = LikeShare.builder()
                    .likeOrDislike(LikeOrDislikeEnums.DISLIKE)
                    .build();

            when(contentRepository.findByIdAndEnableTrue(contentId)).thenReturn(Optional.of(testContent));
            when(likeShareRepository.findByContentIdAndUserId(contentId, userId))
                    .thenReturn(Optional.of(existingDislike));
            when(redisService.get(anyString(), eq(ContentDetailResponse.class))).thenReturn(testDetailResponse);

            contentService.likeContent(request);

            assertThat(existingDislike.getLikeOrDislike()).isEqualTo(LikeOrDislikeEnums.LIKE);
            verify(likeShareRepository).save(existingDislike);
        }

        @Test
        @DisplayName("Should throw exception when content not found")
        void likeContent_WhenContentNotFound_ThrowsException() {
            LikeDislikeRequest request = LikeDislikeRequest.builder()
                    .contentId(contentId)
                    .userId(userId)
                    .build();

            when(contentRepository.findByIdAndEnableTrue(contentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contentService.likeContent(request))
                    .isInstanceOf(ContentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("dislikeContent Tests")
    class DislikeContentTests {

        @Test
        @DisplayName("Should dislike content when no existing like/dislike")
        void dislikeContent_WhenNewDislike_CreatesDislike() {
            LikeDislikeRequest request = LikeDislikeRequest.builder()
                    .contentId(contentId)
                    .userId(userId)
                    .build();

            when(contentRepository.findByIdAndEnableTrue(contentId)).thenReturn(Optional.of(testContent));
            when(likeShareRepository.findByContentIdAndUserId(contentId, userId)).thenReturn(Optional.empty());
            when(redisService.get(anyString(), eq(ContentDetailResponse.class))).thenReturn(testDetailResponse);

            contentService.dislikeContent(request);

            verify(likeShareRepository).save(any(LikeShare.class));
            assertThat(testDetailResponse.getDislikeCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should switch from like to dislike")
        void dislikeContent_WhenPreviouslyLiked_SwitchesToDislike() {
            LikeDislikeRequest request = LikeDislikeRequest.builder()
                    .contentId(contentId)
                    .userId(userId)
                    .build();

            LikeShare existingLike = LikeShare.builder()
                    .likeOrDislike(LikeOrDislikeEnums.LIKE)
                    .build();

            when(contentRepository.findByIdAndEnableTrue(contentId)).thenReturn(Optional.of(testContent));
            when(likeShareRepository.findByContentIdAndUserId(contentId, userId)).thenReturn(Optional.of(existingLike));
            when(redisService.get(anyString(), eq(ContentDetailResponse.class))).thenReturn(testDetailResponse);

            contentService.dislikeContent(request);

            assertThat(existingLike.getLikeOrDislike()).isEqualTo(LikeOrDislikeEnums.DISLIKE);
        }
    }



    @Nested
    @DisplayName("shareContent Tests")
    class ShareContentTests {

        @Test
        @DisplayName("Should share content successfully")
        void shareContent_WithValidIds_SharesContent() {
            when(contentRepository.findByIdAndEnableTrue(contentId)).thenReturn(Optional.of(testContent));
            when(redisService.get(anyString(), eq(ContentDetailResponse.class))).thenReturn(testDetailResponse);

            contentService.shareContent(contentId, userId);

            verify(shareRepository).save(any());
            assertThat(testDetailResponse.getShareCount()).isEqualTo(4L);
        }

        @Test
        @DisplayName("Should throw exception when content ID is null")
        void shareContent_WithNullContentId_ThrowsException() {
            assertThatThrownBy(() -> contentService.shareContent(null, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Content ID is required");
        }

        @Test
        @DisplayName("Should throw exception when user ID is null")
        void shareContent_WithNullUserId_ThrowsException() {
            assertThatThrownBy(() -> contentService.shareContent(contentId, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User ID is required");
        }

        @Test
        @DisplayName("Should throw exception when content not found")
        void shareContent_WhenContentNotFound_ThrowsException() {
            when(contentRepository.findByIdAndEnableTrue(contentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contentService.shareContent(contentId, userId))
                    .isInstanceOf(ContentNotFoundException.class);
        }
    }
}
