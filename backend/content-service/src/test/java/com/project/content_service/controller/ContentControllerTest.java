package com.project.content_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.content_service.domain.dto.request.AddCommentRequest;
import com.project.content_service.domain.dto.request.CreateContentRequest;
import com.project.content_service.domain.dto.request.LikeDislikeRequest;
import com.project.content_service.domain.dto.response.CommentResponse;
import com.project.content_service.domain.dto.response.ContentDetailResponse;
import com.project.content_service.domain.dto.response.ContentResponse;
import com.project.content_service.domain.enums.Category;
import com.project.content_service.domain.enums.Genre;
import com.project.content_service.exception.customException.ContentNotFoundException;
import com.project.content_service.service.ContentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContentController.class)
class ContentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ContentServiceImpl contentService;

        private ObjectMapper objectMapper;
        private UUID contentId;
        private UUID userId;
        private ContentDetailResponse testDetailResponse;
        private ContentResponse testContentResponse;

        @BeforeEach
        void setUp() {
                objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());

                contentId = UUID.randomUUID();
                userId = UUID.randomUUID();

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

                testContentResponse = ContentResponse.builder()
                                .id(contentId)
                                .title("Test Content")
                                .animeCategories(Set.of(Category.MEME))
                                .genre(Set.of(Genre.ACTION))
                                .bio("Test Bio")
                                .enable(true)
                                .created(LocalDateTime.now())
                                .build();
        }

        @Nested
        @DisplayName("GET /content/get Tests")
        class GetContentByIdTests {

                @Test
                @WithMockUser
                @DisplayName("Should return content when found")
                void getContentById_WhenFound_ReturnsContent() throws Exception {
                        when(contentService.getContentDetailById(contentId, null, true))
                                        .thenReturn(testDetailResponse);

                        mockMvc.perform(get("/content/get")
                                        .param("contentId", contentId.toString())
                                        .param("includeMedia", "true"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(contentId.toString()))
                                        .andExpect(jsonPath("$.title").value("Test Content"));
                }

                @Test
                @WithMockUser
                @DisplayName("Should return content without media when includeMedia is false")
                void getContentById_WithoutMedia_ReturnsContentWithoutMedia() throws Exception {
                        when(contentService.getContentDetailById(contentId, null, false))
                                        .thenReturn(testDetailResponse);

                        mockMvc.perform(get("/content/get")
                                        .param("contentId", contentId.toString())
                                        .param("includeMedia", "false"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(contentId.toString()));

                        verify(contentService).getContentDetailById(contentId, null, false);
                }

                @Test
                @WithMockUser
                @DisplayName("Should return content with user like status")
                void getContentById_WithUserId_ReturnsContentWithLikeStatus() throws Exception {
                        testDetailResponse.setIsLiked(true);
                        when(contentService.getContentDetailById(contentId, userId, true))
                                        .thenReturn(testDetailResponse);

                        mockMvc.perform(get("/content/get")
                                        .param("contentId", contentId.toString())
                                        .param("currentUserId", userId.toString())
                                        .param("includeMedia", "true"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.isLiked").value(true));
                }

                @Test
                @WithMockUser
                @DisplayName("Should return 404 when content not found")
                void getContentById_WhenNotFound_Returns404() throws Exception {
                        when(contentService.getContentDetailById(contentId, null, true))
                                        .thenThrow(new ContentNotFoundException("Content not found"));

                        mockMvc.perform(get("/content/get")
                                        .param("contentId", contentId.toString())
                                        .param("includeMedia", "true"))
                                        .andExpect(status().isNotFound());
                }
        }

        @Nested
        @DisplayName("GET /content/get/batch Tests")
        class GetContentsByIdsTests {

                @Test
                @WithMockUser
                @DisplayName("Should return contents for valid IDs")
                void getContentsByIds_WithValidIds_ReturnsContents() throws Exception {
                        Page<ContentDetailResponse> page = new PageImpl<>(List.of(testDetailResponse));
                        when(contentService.getContentsByIds(anyList(), any(), anyInt(), anyBoolean()))
                                        .thenReturn(page);

                        mockMvc.perform(get("/content/get/batch")
                                        .param("contentIds", contentId.toString())
                                        .param("page", "0")
                                        .param("includeMedia", "true"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.content").isArray())
                                        .andExpect(jsonPath("$.content[0].id").value(contentId.toString()));
                }

                @Test
                @WithMockUser
                @DisplayName("Should return empty page when no contents found")
                void getContentsByIds_WhenNoContents_ReturnsEmptyPage() throws Exception {
                        Page<ContentDetailResponse> emptyPage = new PageImpl<>(List.of());
                        when(contentService.getContentsByIds(anyList(), any(), anyInt(), anyBoolean()))
                                        .thenReturn(emptyPage);

                        mockMvc.perform(get("/content/get/batch")
                                        .param("contentIds", contentId.toString())
                                        .param("page", "0")
                                        .param("includeMedia", "true"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.content").isEmpty());
                }
        }

        @Nested
        @DisplayName("GET /content/user Tests")
        class GetContentsByUserIdTests {

                @Test
                @WithMockUser
                @DisplayName("Should return contents for valid user ID")
                void getContentsByUserId_WithValidId_ReturnsContents() throws Exception {
                        Page<ContentDetailResponse> page = new PageImpl<>(List.of(testDetailResponse));
                        when(contentService.getContentsByUserId(eq(userId), any(), anyInt(), anyBoolean()))
                                        .thenReturn(page);

                        mockMvc.perform(get("/content/user")
                                        .param("userId", userId.toString())
                                        .param("page", "0")
                                        .param("includeMedia", "true"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.content").isArray());
                }
        }

        @Nested
        @DisplayName("DELETE /content/delete Tests")
        class DeleteContentTests {

                @Test
                @WithMockUser(roles = "MODERATOR")
                @DisplayName("Should delete content when user is moderator")
                void deleteContent_AsModerator_DeletesContent() throws Exception {
                        doNothing().when(contentService).deleteContent(contentId);

                        mockMvc.perform(delete("/content/delete")
                                        .param("contentId", contentId.toString())
                                        .with(csrf()))
                                        .andExpect(status().isOk());

                        verify(contentService).deleteContent(contentId);
                }

                @Test
                @WithMockUser
                @DisplayName("Should delete content when user is owner")
                void deleteContent_AsOwner_DeletesContent() throws Exception {
                        when(contentService.isOwner(any(), eq(contentId))).thenReturn(true);
                        doNothing().when(contentService).deleteContent(contentId);

                        mockMvc.perform(delete("/content/delete")
                                        .param("contentId", contentId.toString())
                                        .with(csrf()))
                                        .andExpect(status().isOk());
                }

                @Test
                @DisplayName("Should return 401 when not authenticated")
                void deleteContent_WhenNotAuthenticated_Returns401() throws Exception {
                        mockMvc.perform(delete("/content/delete")
                                        .param("contentId", contentId.toString())
                                        .with(csrf()))
                                        .andExpect(status().isUnauthorized());
                }
        }

        @Nested
        @DisplayName("PUT /content/disable Tests")
        class DisableContentTests {

                @Test
                @WithMockUser(roles = "MODERATOR")
                @DisplayName("Should disable content when user is moderator")
                void disableContent_AsModerator_DisablesContent() throws Exception {
                        doNothing().when(contentService).disableContent(contentId);

                        mockMvc.perform(put("/content/disable")
                                        .param("contentId", contentId.toString())
                                        .with(csrf()))
                                        .andExpect(status().isOk());

                        verify(contentService).disableContent(contentId);
                }
        }

        @Nested
        @DisplayName("PUT /content/enable Tests")
        class EnableContentTests {

                @Test
                @WithMockUser(roles = "MODERATOR")
                @DisplayName("Should enable content when user is moderator")
                void enableContent_AsModerator_EnablesContent() throws Exception {
                        doNothing().when(contentService).enableContent(contentId);

                        mockMvc.perform(put("/content/enable")
                                        .param("contentId", contentId.toString())
                                        .with(csrf()))
                                        .andExpect(status().isOk());

                        verify(contentService).enableContent(contentId);
                }
        }

        @Nested
        @DisplayName("GET /content/comments Tests")
        class GetCommentsTests {

                @Test
                @WithMockUser
                @DisplayName("Should return comments for content")
                void getComments_WithValidContentId_ReturnsComments() throws Exception {
                        CommentResponse comment = CommentResponse.builder()
                                        .id(UUID.randomUUID())
                                        .comment("Test comment")
                                        .userId(userId)
                                        .userName("testuser")
                                        .build();
                        Page<CommentResponse> page = new PageImpl<>(List.of(comment));
                        when(contentService.getCommentsByContentId(contentId, 0)).thenReturn(page);

                        mockMvc.perform(get("/content/comments")
                                        .param("contentId", contentId.toString())
                                        .param("page", "0"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.content").isArray())
                                        .andExpect(jsonPath("$.content[0].comment").value("Test comment"));
                }
        }

        @Nested
        @DisplayName("POST /content/comment/add Tests")
        class AddCommentTests {

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should add comment when authenticated")
                void addComment_WhenAuthenticated_AddsComment() throws Exception {
                        AddCommentRequest request = AddCommentRequest.builder()
                                        .contentId(contentId)
                                        .userId(userId)
                                        .userName("testuser")
                                        .comment("Test comment")
                                        .build();

                        doNothing().when(contentService).addComment(any(AddCommentRequest.class));

                        mockMvc.perform(post("/content/comment/add")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request))
                                        .with(csrf()))
                                        .andExpect(status().isCreated());
                }

                @Test
                @DisplayName("Should return 401 when not authenticated")
                void addComment_WhenNotAuthenticated_Returns401() throws Exception {
                        AddCommentRequest request = AddCommentRequest.builder()
                                        .contentId(contentId)
                                        .userId(userId)
                                        .comment("Test comment")
                                        .build();

                        mockMvc.perform(post("/content/comment/add")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request))
                                        .with(csrf()))
                                        .andExpect(status().isUnauthorized());
                }
        }

        @Nested
        @DisplayName("POST /content/like Tests")
        class LikeContentTests {

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should like content when authenticated")
                void likeContent_WhenAuthenticated_LikesContent() throws Exception {
                        LikeDislikeRequest request = LikeDislikeRequest.builder()
                                        .contentId(contentId)
                                        .userId(userId)
                                        .build();

                        doNothing().when(contentService).likeContent(any(LikeDislikeRequest.class));

                        mockMvc.perform(post("/content/like")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request))
                                        .with(csrf()))
                                        .andExpect(status().isOk());
                }
        }

        @Nested
        @DisplayName("POST /content/dislike Tests")
        class DislikeContentTests {

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should dislike content when authenticated")
                void dislikeContent_WhenAuthenticated_DislikesContent() throws Exception {
                        LikeDislikeRequest request = LikeDislikeRequest.builder()
                                        .contentId(contentId)
                                        .userId(userId)
                                        .build();

                        doNothing().when(contentService).dislikeContent(any(LikeDislikeRequest.class));

                        mockMvc.perform(post("/content/dislike")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request))
                                        .with(csrf()))
                                        .andExpect(status().isOk());
                }
        }

        @Nested
        @DisplayName("POST /content/share Tests")
        class ShareContentTests {

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should share content when authenticated")
                void shareContent_WhenAuthenticated_SharesContent() throws Exception {
                        doNothing().when(contentService).shareContent(contentId, userId);

                        mockMvc.perform(post("/content/share")
                                        .param("contentId", contentId.toString())
                                        .param("userId", userId.toString())
                                        .with(csrf()))
                                        .andExpect(status().isCreated());
                }

                @Test
                @DisplayName("Should return 401 when not authenticated")
                void shareContent_WhenNotAuthenticated_Returns401() throws Exception {
                        mockMvc.perform(post("/content/share")
                                        .param("contentId", contentId.toString())
                                        .param("userId", userId.toString())
                                        .with(csrf()))
                                        .andExpect(status().isUnauthorized());
                }
        }
}
