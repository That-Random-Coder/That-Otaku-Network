package com.project.user_service.service;

import com.project.user_service.domain.dto.request.CreateUserDetailsRequestDto;
import com.project.user_service.domain.dto.request.UpdateUserProfileRequestDto;
import com.project.user_service.domain.dto.response.GetFollowResponse;
import com.project.user_service.domain.dto.response.UserProfileResponseDto;
import com.project.user_service.domain.entity.users.Follow;
import com.project.user_service.domain.entity.users.ImageUserEntity;
import com.project.user_service.domain.entity.users.Users;
import com.project.user_service.domain.enums.RedisMethod;
import com.project.user_service.exception.customException.UserAlreadyExistsException;
import com.project.user_service.exception.customException.UserNotFoundException;
import com.project.user_service.repository.FollowRepository;
import com.project.user_service.repository.ImageUserEntityRepository;
import com.project.user_service.repository.UsersRepository;
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

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UsersRepository userRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private KafkaService kafkaService;

    @Mock
    private ImageUserEntityRepository imageUserEntityRepository;

    @InjectMocks
    private UserService userService;

    private UUID testUserId;
    private UUID targetUserId;
    private Users testUser;
    private Users targetUser;
    private CreateUserDetailsRequestDto createUserDto;
    private ImageUserEntity imageUserEntity;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        testUser = Users.builder()
                .id(testUserId)
                .username("testuser")
                .displayName("Test User")
                .bio("Test bio")
                .location("Test Location")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .enable(true)
                .isVerified(false)
                .build();

        targetUser = Users.builder()
                .id(targetUserId)
                .username("targetuser")
                .displayName("Target User")
                .bio("Target bio")
                .location("Target Location")
                .dateOfBirth(LocalDate.of(1999, 5, 15))
                .enable(true)
                .isVerified(false)
                .build();

        imageUserEntity = ImageUserEntity.builder()
                .id(testUserId)
                .image(new byte[] { 1, 2, 3 })
                .imageType("image/jpeg")
                .build();

        createUserDto = new CreateUserDetailsRequestDto();
        createUserDto.setId(testUserId);
        createUserDto.setUsername("testuser");
        createUserDto.setDisplayName("Test User");
        createUserDto.setBio("Test bio");
        createUserDto.setLocation("Test Location");
        createUserDto.setDateOfBirth(LocalDate.of(2000, 1, 1));
    }

    @Nested
    @DisplayName("When creating a user")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user successfully without profile image")
        void testCreateUserWithoutImage() {
            when(userRepository.existsById(testUserId)).thenReturn(false);
            when(userRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserProfileResponseDto result = userService.createUser(createUserDto, null);

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getDisplayName()).isEqualTo("Test User");
            assertThat(result.getFollowers()).isZero();
            assertThat(result.getFollowing()).isZero();
            assertThat(result.getIsVerified()).isFalse();

            verify(userRepository).save(any(Users.class));
            verify(redisService).set(eq(RedisMethod.USER_ + testUserId.toString()), any(UserProfileResponseDto.class),
                    anyLong());
            verify(kafkaService).saveIntoUserDatabase(any(Users.class));
        }

        @Test
        @DisplayName("Should create user successfully with valid profile image")
        void testCreateUserWithImage() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile.jpg",
                    "image/jpeg",
                    "test image content".getBytes());

            when(userRepository.existsById(testUserId)).thenReturn(false);
            when(userRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserProfileResponseDto result = userService.createUser(createUserDto, file);

            assertThat(result).isNotNull();
            assertThat(result.getProfileImg()).isNotNull();
            assertThat(result.getImageType()).isEqualTo("image/jpeg");

            verify(userRepository).save(any(Users.class));
        }

        @Test
        @DisplayName("Should throw exception when user already exists")
        void testCreateUserAlreadyExists() {
            when(userRepository.existsById(testUserId)).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(createUserDto, null))
                    .isInstanceOf(UserAlreadyExistsException.class);

            verify(userRepository, never()).save(any(Users.class));
        }

        @Test
        @DisplayName("Should throw exception for invalid image type")
        void testCreateUserWithInvalidImageType() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile.gif",
                    "image/gif",
                    "test image content".getBytes());

            when(userRepository.existsById(testUserId)).thenReturn(false);

            assertThatThrownBy(() -> userService.createUser(createUserDto, file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only JPEG, PNG, or WEBP images are allowed");
        }

        @Test
        @DisplayName("Should throw exception for image too large")
        void testCreateUserWithImageTooLarge() {
            byte[] largeImage = new byte[6 * 1024 * 1024]; // 6 MB
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile.jpg",
                    "image/jpeg",
                    largeImage);

            when(userRepository.existsById(testUserId)).thenReturn(false);

            assertThatThrownBy(() -> userService.createUser(createUserDto, file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Profile image must be less than 5 MB");
        }
    }

    @Nested
    @DisplayName("When getting user profile")
    class GetProfileTests {

        @Test
        @DisplayName("Should return profile from cache when available")
        void testGetProfileFromCache() {
            UserProfileResponseDto cachedProfile = UserProfileResponseDto.builder()
                    .id(testUserId)
                    .username("testuser")
                    .displayName("Test User")
                    .followers(10)
                    .following(5)
                    .build();

            when(redisService.get(RedisMethod.USER_ + testUserId.toString(), UserProfileResponseDto.class))
                    .thenReturn(cachedProfile);

            UserProfileResponseDto result = userService.getProfile(testUserId, false);

            assertThat(result).isEqualTo(cachedProfile);
            verify(redisService).set(eq(RedisMethod.USER_ + testUserId.toString()), any(UserProfileResponseDto.class),
                    anyLong());
            verify(userRepository, never()).getUserWithDetails(any());
        }

        @Test
        @DisplayName("Should fetch from database when not in cache")
        void testGetProfileFromDatabase() {
            UserProfileResponseDto dbProfile = UserProfileResponseDto.builder()
                    .id(testUserId)
                    .username("testuser")
                    .displayName("Test User")
                    .bio("Test bio")
                    .followers(10)
                    .following(5)
                    .isVerified(false)
                    .build();

            when(redisService.get(anyString(), eq(UserProfileResponseDto.class))).thenReturn(null);
            when(userRepository.getUserWithDetails(testUserId)).thenReturn(Optional.of(dbProfile));

            UserProfileResponseDto result = userService.getProfile(testUserId, false);

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            verify(userRepository).getUserWithDetails(testUserId);
            verify(redisService).set(anyString(), any(UserProfileResponseDto.class), anyLong());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void testGetProfileUserNotFound() {
            when(redisService.get(anyString(), eq(UserProfileResponseDto.class))).thenReturn(null);
            when(userRepository.getUserWithDetails(testUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfile(testUserId, false))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("When updating user profile")
    class UpdateUserProfileTests {

        @Test
        @DisplayName("Should update all fields successfully")
        void testUpdateAllFields() {
            UpdateUserProfileRequestDto updateDto = new UpdateUserProfileRequestDto();
            updateDto.setDisplayName("New Display Name");
            updateDto.setBio("New bio");
            updateDto.setLocation("New Location");
            updateDto.setDateOfBirth(LocalDate.of(1995, 3, 20));

            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(Users.class))).thenReturn(testUser);

            List<String> result = userService.updateUserProfile(testUserId, updateDto);

            assertThat(testUser.getDisplayName()).isEqualTo("New Display Name");
            assertThat(testUser.getBio()).isEqualTo("New bio");
            assertThat(testUser.getLocation()).isEqualTo("New Location");
            assertThat(result).isNotNull()
                    .hasSize(4)
                    .contains("Display Name", "Bio", "Location", "Date of Birth");

            verify(userRepository).findById(testUserId);
            verify(userRepository).save(testUser);
            verify(redisService).delete(RedisMethod.USER_ + testUserId.toString());
            verify(kafkaService).updateIntoUserDatabase(eq(testUserId), eq("New Display Name"), eq("New bio"),
                    eq("testuser"));
        }

        @Test
        @DisplayName("Should update only provided fields")
        void testUpdatePartialFields() {
            UpdateUserProfileRequestDto updateDto = new UpdateUserProfileRequestDto();
            updateDto.setDisplayName("New Display Name");

            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(Users.class))).thenReturn(testUser);

            List<String> result = userService.updateUserProfile(testUserId, updateDto);

            assertThat(testUser.getDisplayName()).isEqualTo("New Display Name");
            assertThat(testUser.getBio()).isEqualTo("Test bio"); // Unchanged
            assertThat(result).isNotNull()
                    .hasSize(1)
                    .contains("Display Name");

            verify(userRepository).findById(testUserId);
            verify(userRepository).save(testUser);
            verify(redisService).delete(RedisMethod.USER_ + testUserId.toString());
            verify(kafkaService).updateIntoUserDatabase(eq(testUserId), eq("New Display Name"), isNull(),
                    eq("testuser"));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void testUpdateUserNotFound() {
            UpdateUserProfileRequestDto updateDto = new UpdateUserProfileRequestDto();
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUserProfile(testUserId, updateDto))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when user is disabled")
        void testUpdateDisabledUser() {
            testUser.setEnable(false);
            UpdateUserProfileRequestDto updateDto = new UpdateUserProfileRequestDto();
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> userService.updateUserProfile(testUserId, updateDto))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("When following/unfollowing users")
    class FollowTests {

        @Test
        @DisplayName("Should follow user successfully")
        void testFollowUser() {
            when(userRepository.findByIdAndEnableTrue(testUserId)).thenReturn(testUser);
            when(userRepository.findByIdAndEnableTrue(targetUserId)).thenReturn(targetUser);
            when(followRepository.existsByFollowerAndFollowing(testUser, targetUser)).thenReturn(false);

            userService.followUser(testUserId, targetUserId);

            verify(followRepository).save(any(Follow.class));
            verify(redisService).delete(RedisMethod.USER_ + testUserId.toString());
            verify(redisService).delete(RedisMethod.USER_ + targetUserId.toString());
        }

        @Test
        @DisplayName("Should throw exception when trying to follow self")
        void testFollowSelf() {
            assertThatThrownBy(() -> userService.followUser(testUserId, testUserId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("You cannot follow yourself");
        }

        @Test
        @DisplayName("Should throw exception when already following")
        void testFollowAlreadyFollowing() {
            when(userRepository.findByIdAndEnableTrue(testUserId)).thenReturn(testUser);
            when(userRepository.findByIdAndEnableTrue(targetUserId)).thenReturn(targetUser);
            when(followRepository.existsByFollowerAndFollowing(testUser, targetUser)).thenReturn(true);

            assertThatThrownBy(() -> userService.followUser(testUserId, targetUserId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Already following this user");
        }

        @Test
        @DisplayName("Should throw exception when follower user not found")
        void testFollowFollowerNotFound() {
            when(userRepository.findByIdAndEnableTrue(testUserId)).thenReturn(null);

            assertThatThrownBy(() -> userService.followUser(testUserId, targetUserId))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when target user not found")
        void testFollowTargetNotFound() {
            when(userRepository.findByIdAndEnableTrue(testUserId)).thenReturn(testUser);
            when(userRepository.findByIdAndEnableTrue(targetUserId)).thenReturn(null);

            assertThatThrownBy(() -> userService.followUser(testUserId, targetUserId))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Should unfollow user successfully")
        void testUnfollowUser() {
            when(userRepository.findByIdAndEnableTrue(testUserId)).thenReturn(testUser);
            when(userRepository.findByIdAndEnableTrue(targetUserId)).thenReturn(targetUser);
            when(followRepository.deleteFollowerAndFollowing(testUser, targetUser)).thenReturn(1);

            userService.unfollowUser(testUserId, targetUserId);

            verify(followRepository).deleteFollowerAndFollowing(testUser, targetUser);
            verify(redisService).delete(RedisMethod.USER_ + testUserId.toString());
            verify(redisService).delete(RedisMethod.USER_ + targetUserId.toString());
        }

        @Test
        @DisplayName("Should throw exception when not following user")
        void testUnfollowNotFollowing() {
            when(userRepository.findByIdAndEnableTrue(testUserId)).thenReturn(testUser);
            when(userRepository.findByIdAndEnableTrue(targetUserId)).thenReturn(targetUser);
            when(followRepository.deleteFollowerAndFollowing(testUser, targetUser)).thenReturn(0);

            assertThatThrownBy(() -> userService.unfollowUser(testUserId, targetUserId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("You are not following this user");
        }
    }

    @Nested
    @DisplayName("When uploading profile image")
    class UploadImageTests {

        @Test
        @DisplayName("Should upload image successfully")
        void testUploadImageSuccess() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile.jpg",
                    "image/jpeg",
                    "test image content".getBytes());

            when(userRepository.existsByIdAndEnableTrue(testUserId)).thenReturn(true);
            when(imageUserEntityRepository.findById(testUserId)).thenReturn(Optional.of(imageUserEntity));

            boolean result = userService.uploadImage(testUserId, file);

            assertThat(result).isTrue();
            verify(imageUserEntityRepository).save(any(ImageUserEntity.class));
            verify(redisService).delete(RedisMethod.USER_ + testUserId.toString());
        }

        @Test
        @DisplayName("Should throw exception for null file")
        void testUploadImageNullFile() {
            assertThatThrownBy(() -> userService.uploadImage(testUserId, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void testUploadImageUserNotFound() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile.jpg",
                    "image/jpeg",
                    "test image content".getBytes());

            when(userRepository.existsByIdAndEnableTrue(testUserId)).thenReturn(false);

            assertThatThrownBy(() -> userService.uploadImage(testUserId, file))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("When getting followers and following")
    class GetFollowersAndFollowingTests {

        @Test
        @DisplayName("Should get followers list")
        void testGetFollowers() {
            GetFollowResponse follower1 = new GetFollowResponse(UUID.randomUUID(), "user1", "User 1", false);
            GetFollowResponse follower2 = new GetFollowResponse(UUID.randomUUID(), "user2", "User 2", true);

            Page<GetFollowResponse> page = new PageImpl<>(Arrays.asList(follower1, follower2));
            when(followRepository.getFollower(eq(testUserId), any(Pageable.class))).thenReturn(page);

            Page<GetFollowResponse> result = userService.getFollower(testUserId, 0);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(followRepository).getFollower(eq(testUserId), any(Pageable.class));
        }

        @Test
        @DisplayName("Should get following list")
        void testGetFollowing() {
            GetFollowResponse following1 = new GetFollowResponse(UUID.randomUUID(), "user1", "User 1", false);
            GetFollowResponse following2 = new GetFollowResponse(UUID.randomUUID(), "user2", "User 2", true);

            Page<GetFollowResponse> page = new PageImpl<>(Arrays.asList(following1, following2));
            when(followRepository.getFollowing(eq(testUserId), any(Pageable.class))).thenReturn(page);

            Page<GetFollowResponse> result = userService.getFollowing(testUserId, 0);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(followRepository).getFollowing(eq(testUserId), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle negative page number")
        void testGetFollowersWithNegativePage() {
            Page<GetFollowResponse> page = new PageImpl<>(Collections.emptyList());
            when(followRepository.getFollower(eq(testUserId), any(Pageable.class))).thenReturn(page);

            Page<GetFollowResponse> result = userService.getFollower(testUserId, -1);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();

        }

        @Test
        @DisplayName("Should throw exception for null user id in getFollowing")
        void testGetFollowingNullId() {
            assertThatThrownBy(() -> userService.getFollowing(null, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
