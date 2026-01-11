package com.project.user_service.service;

import com.project.user_service.domain.dto.request.BioUpdateGroupRequestDto;
import com.project.user_service.domain.dto.request.CreateGroupRequestDto;
import com.project.user_service.domain.dto.response.GroupResponseDto;
import com.project.user_service.domain.entity.groups.Group;
import com.project.user_service.domain.entity.groups.GroupMember;
import com.project.user_service.domain.entity.groups.ImageGroup;
import com.project.user_service.domain.entity.users.Users;
import com.project.user_service.domain.enums.RedisMethod;
import com.project.user_service.exception.customException.GroupNotFoundException;
import com.project.user_service.exception.customException.ImageUploadFailedException;
import com.project.user_service.exception.customException.UserNotFoundException;
import com.project.user_service.repository.GroupMemberRepository;
import com.project.user_service.repository.GroupRepository;
import com.project.user_service.repository.ImageGroupRepository;
import com.project.user_service.repository.UsersRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {
        @Mock
        private org.springframework.security.core.Authentication authentication;
        @Mock
        private org.springframework.security.core.context.SecurityContext securityContext;

        @Mock
        private UsersRepository usersRepository;

        @Mock
        private GroupRepository groupRepository;

        @Mock
        private RedisService redisService;

        @Mock
        private ImageGroupRepository imageGroupRepository;

        @Mock
        private GroupMemberRepository groupMemberRepository;

        @Mock
        private KafkaService kafkaService;

        @InjectMocks
        private GroupService groupService;

        private Users user;
        private Users otherUser;
        private Group group1;
        private CreateGroupRequestDto createGroupRequestDto;

        @BeforeEach
        void setUp() {
                user = Users
                                .builder()
                                .id(UUID.randomUUID())
                                .username("test_user_1")
                                .bio("test user bio")
                                .displayName("Test User")
                                .location("Test Location")
                                .dateOfBirth(LocalDate.of(2025, 5, 25))
                                .leaderOfGroup(new HashSet<>())
                                .enable(true)
                                .build();

                otherUser = Users
                                .builder()
                                .id(UUID.randomUUID())
                                .username("other_user")
                                .bio("other user bio")
                                .displayName("Other User")
                                .location("Other Location")
                                .dateOfBirth(LocalDate.of(2024, 3, 15))
                                .leaderOfGroup(new HashSet<>())
                                .enable(true)
                                .build();

                group1 = Group
                                .builder()
                                .id(UUID.randomUUID())
                                .groupName("Test_group")
                                .bio("test bio")
                                .leader(user)
                                .members(new HashSet<>())
                                .enable(true)
                                .build();

                createGroupRequestDto = CreateGroupRequestDto
                                .builder()
                                .groupName(group1.getGroupName())
                                .leaderId(user.getId())
                                .groupBio(group1.getBio())
                                .build();
        }

        @Nested
        @DisplayName("When creating a Group")
        class CreateGroupTest {

                @Test
                @DisplayName("Should Create A group without Any problem with null image")
                void testCreateAGroupWithNullImage() {
                        when(usersRepository.findByIdAndEnableTrue(any())).thenReturn(user);
                        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> {
                                Group group = invocation.getArgument(0);
                                if (group.getId() == null) {
                                        group.setId(UUID.randomUUID());
                                }
                                return group;
                        });

                        GroupResponseDto requestDto = groupService.createGroup(createGroupRequestDto, null, null);

                        assertNotNull(requestDto);
                        assertEquals(createGroupRequestDto.getGroupName(), requestDto.getGroupName());
                        assertEquals(createGroupRequestDto.getGroupBio(), requestDto.getGroupBio());
                        assertNotNull(requestDto.getId());
                        assertEquals(1, requestDto.getMemberCount());

                        verify(redisService).set(eq(RedisMethod.GROUP_ + requestDto.getId().toString()),
                                        any(GroupResponseDto.class), anyLong());
                }

                @Test
                @DisplayName("Should throw exception as User not Found")
                void testCreateAGroupWithUserNotFound() {
                        when(usersRepository.findByIdAndEnableTrue(any())).thenReturn(null);

                        assertThatThrownBy(
                                        () -> groupService.createGroup(createGroupRequestDto, null, null))
                                        .isInstanceOf(UserNotFoundException.class);

                        verify(groupRepository, never()).save(any(Group.class));
                        verify(redisService, never()).set(any(), any(), anyLong());
                }

                @Test
                @DisplayName("Should create a group with Image")
                void testCreateGroupWithImage() {
                        when(usersRepository.findByIdAndEnableTrue(any())).thenReturn(user);
                        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> {
                                Group group = invocation.getArgument(0);
                                if (group.getId() == null) {
                                        group.setId(UUID.randomUUID());
                                }
                                return group;
                        });

                        byte[] largeImage = new byte[5 * 1024 * 1024];
                        MockMultipartFile file = new MockMultipartFile(
                                        "file",
                                        "profile.jpg",
                                        "image/jpeg",
                                        largeImage);

                        GroupResponseDto requestDto = groupService.createGroup(createGroupRequestDto, file, file);

                        assertNotNull(requestDto);
                        assertEquals(createGroupRequestDto.getGroupName(), requestDto.getGroupName());
                        assertEquals(createGroupRequestDto.getGroupBio(), requestDto.getGroupBio());
                        assertNotNull(requestDto.getId());

                        verify(groupRepository).save(any(Group.class));
                        verify(redisService).set(eq(RedisMethod.GROUP_ + requestDto.getId().toString()),
                                        any(GroupResponseDto.class), anyLong());
                }

                @Test
                @DisplayName("Should throw Exception as file is not image")
                void testCreateGroupWithWrongFile() {
                        when(usersRepository.findByIdAndEnableTrue(any())).thenReturn(user);

                        byte[] largeImage = new byte[5 * 1024 * 1024];
                        MockMultipartFile file = new MockMultipartFile(
                                        "file",
                                        "profile.gif",
                                        "image/gif",
                                        largeImage);

                        assertThatThrownBy(
                                        () -> groupService.createGroup(createGroupRequestDto, file, null))
                                        .isInstanceOf(IllegalArgumentException.class);

                        verify(groupRepository, never()).save(any(Group.class));
                        verify(redisService, never()).set(any(), any(), anyLong());
                }

                @Test
                @DisplayName("Should throw Exception as file is more than 5 Mb")
                void testCreateGroupWithLargeFile() {
                        when(usersRepository.findByIdAndEnableTrue(any())).thenReturn(user);

                        byte[] largeImage = new byte[10 * 1024 * 1024];
                        MockMultipartFile file = new MockMultipartFile(
                                        "file",
                                        "profile.png",
                                        "image/png",
                                        largeImage);

                        assertThatThrownBy(
                                        () -> groupService.createGroup(createGroupRequestDto, file, null))
                                        .isInstanceOf(IllegalArgumentException.class);

                        verify(groupRepository, never()).save(any(Group.class));
                        verify(redisService, never()).set(any(), any(), anyLong());
                }

                @Test
                @DisplayName("Should throw ImageUploadFailedException when IOException occurs for Profile Image")
                void testCreateGroupWithIOExceptionWithProfileImage() throws Exception {
                        when(usersRepository.findByIdAndEnableTrue(any())).thenReturn(user);

                        MultipartFile mockFile = mock(MultipartFile.class);
                        when(mockFile.isEmpty()).thenReturn(false);
                        when(mockFile.getSize()).thenReturn(1024L);
                        when(mockFile.getContentType()).thenReturn("image/jpeg");
                        when(mockFile.getBytes()).thenThrow(new java.io.IOException("File read error"));

                        assertThatThrownBy(
                                        () -> groupService.createGroup(createGroupRequestDto, mockFile, null))
                                        .isInstanceOf(ImageUploadFailedException.class)
                                        .hasMessageContaining("Failed to profile images");

                        verify(groupRepository, never()).save(any(Group.class));
                        verify(redisService, never()).set(any(), any(), anyLong());
                }

                @Test
                @DisplayName("Should throw ImageUploadFailedException when IOException occurs for Bg Image")
                void testCreateGroupWithIOExceptionWithBgImage() throws Exception {
                        when(usersRepository.findByIdAndEnableTrue(any())).thenReturn(user);

                        MultipartFile mockFile = mock(MultipartFile.class);
                        when(mockFile.isEmpty()).thenReturn(false);
                        when(mockFile.getSize()).thenReturn(1024L);
                        when(mockFile.getContentType()).thenReturn("image/jpeg");
                        when(mockFile.getBytes()).thenThrow(new java.io.IOException("File read error"));

                        assertThatThrownBy(
                                        () -> groupService.createGroup(createGroupRequestDto, null, mockFile))
                                        .isInstanceOf(ImageUploadFailedException.class)
                                        .hasMessageContaining("Failed to background images");

                        verify(groupRepository, never()).save(any(Group.class));
                        verify(redisService, never()).set(any(), any(), anyLong());
                }
        }

        @Nested
        @DisplayName("When Group Get Request")
        class GetGroupTest {

                @Test
                @DisplayName("Should get the User from the Database")
                void testGetGroupFromDataBase() {
                        // Mock SecurityContextHolder
                        org.mockito.Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
                        org.mockito.Mockito.when(authentication.getPrincipal())
                                        .thenReturn(new com.project.user_service.domain.security.UserDetailCustom(
                                                        java.util.UUID.randomUUID(),
                                                        com.project.user_service.domain.enums.Roles.USER));
                        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
                        when(redisService.get(any(), any())).thenReturn(null);

                        GroupResponseDto groupResponseDto = GroupResponseDto
                                        .builder()
                                        .id(group1.getId())
                                        .groupName("Test_Group")
                                        .groupBio("Test_bio")
                                        .leaderId(user.getId())
                                        .leaderUsername(user.getUsername())
                                        .leaderDisplayName(user.getDisplayName())
                                        .memberCount(1)
                                        .build();

                        when(groupRepository.getGroupById(eq(group1.getId()), any()))
                                        .thenReturn(Optional.of(groupResponseDto));
                        when(groupRepository.findById(eq(group1.getId()))).thenReturn(Optional.of(group1));

                        GroupResponseDto responseDto = groupService.getGroup(group1.getId(), false);

                        assertNotNull(responseDto);
                        assertEquals(groupResponseDto.getGroupName(), responseDto.getGroupName());
                        verify(redisService, times(1)).set(eq(RedisMethod.GROUP_ + group1.getId().toString()), any(),
                                        anyLong());
                        verify(groupRepository).getGroupById(eq(group1.getId()), any());
                }

                @Test
                @DisplayName("Should throw an Exception For No Id")
                void testGetGroupWithNoId() {
                        assertThatThrownBy(() -> groupService.getGroup(null, true))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Id can't be Null");

                        verify(redisService, never()).set(any(), any(), anyLong());
                        verify(groupRepository, never()).getGroupById(any(), any());
                }

                @Test
                @DisplayName("Should throw GroupNotFoundException when group not found")
                void testGetGroupNotFound() {
                        // Mock SecurityContextHolder
                        org.mockito.Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
                        org.mockito.Mockito.when(authentication.getPrincipal())
                                        .thenReturn(new com.project.user_service.domain.security.UserDetailCustom(
                                                        java.util.UUID.randomUUID(),
                                                        com.project.user_service.domain.enums.Roles.USER));
                        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
                        UUID groupId = UUID.randomUUID();
                        when(redisService.get(any(), any())).thenReturn(null);
                        when(groupRepository.getGroupById(eq(groupId), any())).thenReturn(Optional.empty());

                        assertThatThrownBy(() -> groupService.getGroup(groupId, false))
                                        .isInstanceOf(GroupNotFoundException.class);
                }

                @Test
                @DisplayName("Should throw exception when group is disabled")
                void testGetDisabledGroup() {
                        // Mock SecurityContextHolder
                        org.mockito.Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
                        org.mockito.Mockito.when(authentication.getPrincipal())
                                        .thenReturn(new com.project.user_service.domain.security.UserDetailCustom(
                                                        java.util.UUID.randomUUID(),
                                                        com.project.user_service.domain.enums.Roles.USER));
                        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
                        Group disabledGroup = Group.builder()
                                        .id(UUID.randomUUID())
                                        .groupName("Disabled Group")
                                        .bio("Disabled")
                                        .leader(user)
                                        .members(new HashSet<>())
                                        .enable(false)
                                        .build();

                        GroupResponseDto dto = GroupResponseDto.builder()
                                        .id(disabledGroup.getId())
                                        .groupName("Disabled Group")
                                        .groupBio("Disabled")
                                        .memberCount(1)
                                        .build();

                        when(redisService.get(any(), any())).thenReturn(null);
                        when(groupRepository.getGroupById(eq(disabledGroup.getId()), any()))
                                        .thenReturn(Optional.of(dto));
                        when(groupRepository.findById(eq(disabledGroup.getId())))
                                        .thenReturn(Optional.of(disabledGroup));

                        assertThatThrownBy(() -> groupService.getGroup(disabledGroup.getId(), false))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Group is Block");
                }

                @Test
                @DisplayName("Should get group with images")
                void testGetGroupWithImages() {
                        // Mock SecurityContextHolder
                        org.mockito.Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
                        org.mockito.Mockito.when(authentication.getPrincipal())
                                        .thenReturn(new com.project.user_service.domain.security.UserDetailCustom(
                                                        java.util.UUID.randomUUID(),
                                                        com.project.user_service.domain.enums.Roles.USER));
                        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
                        UUID groupId = UUID.randomUUID();
                        byte[] imageData = new byte[] { 1, 2, 3 };

                        ImageGroup imageGroup = ImageGroup.builder()
                                        .profileImage(imageData)
                                        .profileImageType("image/jpeg")
                                        .bgImage(imageData)
                                        .bgImageType("image/jpeg")
                                        .build();

                        GroupResponseDto dto = GroupResponseDto.builder()
                                        .id(groupId)
                                        .groupName("Test Group")
                                        .groupBio("Bio")
                                        .memberCount(1)
                                        .build();

                        Group group = Group.builder()
                                        .id(groupId)
                                        .groupName("Test Group")
                                        .bio("Bio")
                                        .leader(user)
                                        .members(new HashSet<>())
                                        .enable(true)
                                        .build();

                        when(redisService.get(any(), any())).thenReturn(null);
                        when(groupRepository.getGroupById(eq(groupId), any())).thenReturn(Optional.of(dto));
                        when(groupRepository.findById(eq(groupId))).thenReturn(Optional.of(group));
                        when(imageGroupRepository.findById(eq(groupId))).thenReturn(Optional.of(imageGroup));

                        GroupResponseDto result = groupService.getGroup(groupId, true);

                        assertNotNull(result);
                        assertNotNull(result.getProfileImage());
                        assertNotNull(result.getBgImage());
                }
        }

        @Nested
        @DisplayName("When updating group images")
        class UpdateImageTest {

                @Test
                @DisplayName("Should update profile image successfully")
                void testUpdateProfileImage() {
                        ImageGroup imageGroup = ImageGroup.builder()
                                        .profileImage(null)
                                        .profileImageType(null)
                                        .build();

                        byte[] imageData = new byte[1024];
                        MockMultipartFile profileImage = new MockMultipartFile(
                                        "profileImage",
                                        "profile.jpg",
                                        "image/jpeg",
                                        imageData);

                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.of(group1));
                        when(groupRepository.getGroupImageById(group1.getId())).thenReturn(imageGroup);
                        when(imageGroupRepository.save(any(ImageGroup.class))).thenReturn(imageGroup);

                        List<String> result = groupService.updateImage(group1.getId(), profileImage, null);

                        assertTrue(result.contains("Profile Image"));
                        verify(imageGroupRepository).save(imageGroup);
                        verify(redisService).delete(RedisMethod.GROUP_ + group1.getId().toString());
                }

                @Test
                @DisplayName("Should update both images successfully")
                void testUpdateBothImages() {
                        ImageGroup imageGroup = ImageGroup.builder().build();

                        byte[] imageData = new byte[1024];
                        MockMultipartFile profileImage = new MockMultipartFile(
                                        "profileImage",
                                        "profile.jpg",
                                        "image/jpeg",
                                        imageData);
                        MockMultipartFile bgImage = new MockMultipartFile(
                                        "bgImage",
                                        "bg.jpg",
                                        "image/jpeg",
                                        imageData);

                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.of(group1));
                        when(groupRepository.getGroupImageById(group1.getId())).thenReturn(imageGroup);
                        when(imageGroupRepository.save(any(ImageGroup.class))).thenReturn(imageGroup);

                        List<String> result = groupService.updateImage(group1.getId(), profileImage, bgImage);

                        assertEquals(2, result.size());
                        assertTrue(result.contains("Profile Image"));
                        assertTrue(result.contains("Background Image"));
                }

                @Test
                @DisplayName("Should throw exception when group id is null")
                void testUpdateImageWithNullId() {
                        assertThatThrownBy(() -> groupService.updateImage(null, null, null))
                                        .isInstanceOf(GroupNotFoundException.class)
                                        .hasMessageContaining("Group Id cannot be null");
                }

                @Test
                @DisplayName("Should throw exception when group is disabled")
                void testUpdateImageDisabledGroup() {
                        Group disabledGroup = Group.builder()
                                        .id(UUID.randomUUID())
                                        .groupName("Disabled")
                                        .bio("Disabled")
                                        .leader(user)
                                        .members(new HashSet<>())
                                        .enable(false)
                                        .build();

                        when(groupRepository.findById(disabledGroup.getId())).thenReturn(Optional.of(disabledGroup));

                        assertThatThrownBy(() -> groupService.updateImage(disabledGroup.getId(), null, null))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Group is Block");
                }

                @Test
                @DisplayName("Should throw exception for invalid image type")
                void testUpdateImageInvalidType() {
                        MockMultipartFile invalidFile = new MockMultipartFile(
                                        "file",
                                        "file.gif",
                                        "image/gif",
                                        new byte[1024]);

                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.of(group1));
                        when(groupRepository.getGroupImageById(group1.getId())).thenReturn(new ImageGroup());

                        assertThatThrownBy(() -> groupService.updateImage(group1.getId(), invalidFile, null))
                                        .isInstanceOf(IllegalArgumentException.class);
                }

                @Test
                @DisplayName("Should throw exception for oversized image")
                void testUpdateImageOversized() {
                        byte[] largeImage = new byte[10 * 1024 * 1024];
                        MockMultipartFile oversizedFile = new MockMultipartFile(
                                        "file",
                                        "large.jpg",
                                        "image/jpeg",
                                        largeImage);

                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.of(group1));
                        when(groupRepository.getGroupImageById(group1.getId())).thenReturn(new ImageGroup());

                        assertThatThrownBy(() -> groupService.updateImage(group1.getId(), oversizedFile, null))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Profile image must be less than 5 MB");
                }
        }

        @Nested
        @DisplayName("When updating group bio")
        class UpdateBioTest {

                @Test
                @DisplayName("Should update bio successfully")
                void testUpdateBioSuccess() {
                        BioUpdateGroupRequestDto requestDto = BioUpdateGroupRequestDto.builder()
                                        .id(group1.getId())
                                        .bio("New bio")
                                        .build();

                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.of(group1));
                        when(groupRepository.updateTheBio(group1.getId(), "New bio")).thenReturn(1);

                        List<String> result = groupService.updateBio(requestDto);

                        assertTrue(result.contains("Bio"));
                        verify(groupRepository).updateTheBio(group1.getId(), "New bio");
                        verify(redisService).delete(RedisMethod.GROUP_ + group1.getId().toString());
                        verify(kafkaService).updateIntoGroupDatabase(eq(group1.getId()), isNull(), eq("New bio"),
                                        isNull(),
                                        isNull());
                }

                @Test
                @DisplayName("Should throw exception when id is null")
                void testUpdateBioNullId() {
                        BioUpdateGroupRequestDto requestDto = BioUpdateGroupRequestDto.builder()
                                        .id(null)
                                        .bio("New bio")
                                        .build();

                        assertThatThrownBy(() -> groupService.updateBio(requestDto))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Request Missing Id or Bio");
                }

                @Test
                @DisplayName("Should throw exception when bio is null")
                void testUpdateBioNullBio() {
                        BioUpdateGroupRequestDto requestDto = BioUpdateGroupRequestDto.builder()
                                        .id(group1.getId())
                                        .bio(null)
                                        .build();

                        assertThatThrownBy(() -> groupService.updateBio(requestDto))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Request Missing Id or Bio");
                }

                @Test
                @DisplayName("Should throw exception when group not found")
                void testUpdateBioGroupNotFound() {
                        BioUpdateGroupRequestDto requestDto = BioUpdateGroupRequestDto.builder()
                                        .id(group1.getId())
                                        .bio("New bio")
                                        .build();

                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.empty());

                        assertThatThrownBy(() -> groupService.updateBio(requestDto))
                                        .isInstanceOf(GroupNotFoundException.class);
                }

                @Test
                @DisplayName("Should throw exception when group is disabled")
                void testUpdateBioDisabledGroup() {
                        Group disabledGroup = Group.builder()
                                        .id(UUID.randomUUID())
                                        .groupName("Disabled")
                                        .bio("Disabled")
                                        .leader(user)
                                        .members(new HashSet<>())
                                        .enable(false)
                                        .build();

                        BioUpdateGroupRequestDto requestDto = BioUpdateGroupRequestDto.builder()
                                        .id(disabledGroup.getId())
                                        .bio("New bio")
                                        .build();

                        when(groupRepository.findById(disabledGroup.getId())).thenReturn(Optional.of(disabledGroup));

                        assertThatThrownBy(() -> groupService.updateBio(requestDto))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Group is Block");
                }
        }

        @Nested
        @DisplayName("When checking if user is leader")
        class IsLeaderTest {

                @Test
                @DisplayName("Should return true when user is leader")
                void testIsLeaderTrue() {
                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.of(group1));

                        boolean result = groupService.isLeader(user.getId(), group1.getId());

                        assertTrue(result);
                }

                @Test
                @DisplayName("Should return false when user is not leader")
                void testIsLeaderFalse() {
                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.of(group1));

                        boolean result = groupService.isLeader(otherUser.getId(), group1.getId());

                        assertFalse(result);
                }

                @Test
                @DisplayName("Should return false when group not found")
                void testIsLeaderGroupNotFound() {
                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.empty());

                        boolean result = groupService.isLeader(user.getId(), group1.getId());

                        assertFalse(result);
                }

                @Test
                @DisplayName("Should return false when parameters are null")
                void testIsLeaderNullParameters() {
                        assertFalse(groupService.isLeader(null, group1.getId()));
                        assertFalse(groupService.isLeader(user.getId(), null));
                        assertFalse(groupService.isLeader(null, null));
                }
        }

        @Nested
        @DisplayName("When changing group leader")
        class ChangeLeaderTest {

                @Test
                @DisplayName("Should change leader successfully")
                void testChangeLeaderSuccess() {
                        GroupMember member = GroupMember.builder()
                                        .id(UUID.randomUUID())
                                        .users(otherUser)
                                        .group(group1)
                                        .build();

                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.of(group1));
                        when(usersRepository.findByIdAndEnableTrue(otherUser.getId())).thenReturn(otherUser);
                        when(groupMemberRepository.findByGroupIdAndUserId(group1.getId(), otherUser.getId()))
                                        .thenReturn(Optional.of(member));
                        when(groupMemberRepository.findByGroupIdAndUserId(group1.getId(), user.getId()))
                                        .thenReturn(Optional.empty());
                        when(groupRepository.save(any(Group.class))).thenReturn(group1);

                        GroupResponseDto result = groupService.changeLeader(group1.getId(), otherUser.getId(),
                                        user.getId());

                        assertNotNull(result);
                        assertEquals(otherUser.getId(), result.getLeaderId());
                        assertEquals(otherUser.getUsername(), result.getLeaderUsername());
                        verify(groupMemberRepository).delete(member);
                        verify(groupMemberRepository).save(any(GroupMember.class));
                        verify(groupRepository).save(group1);
                        verify(redisService).delete(RedisMethod.GROUP_ + group1.getId().toString());
                }

                @Test
                @DisplayName("Should throw exception when new leader is same as current")
                void testChangeLeaderSameUser() {
                        assertThatThrownBy(() -> groupService.changeLeader(group1.getId(), user.getId(), user.getId()))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("New leader cannot be the same as current leader");
                }

                @Test
                @DisplayName("Should throw exception when group not found")
                void testChangeLeaderGroupNotFound() {
                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.empty());

                        assertThatThrownBy(() -> groupService.changeLeader(group1.getId(), otherUser.getId(),
                                        user.getId()))
                                        .isInstanceOf(GroupNotFoundException.class);
                }

                @Test
                @DisplayName("Should throw exception when not current leader")
                void testChangeLeaderNotCurrentLeader() {
                        UUID thirdUserId = UUID.randomUUID();
                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.of(group1));

                        assertThatThrownBy(
                                        () -> groupService.changeLeader(group1.getId(), otherUser.getId(), thirdUserId))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Only the current leader can transfer leadership");
                }

                @Test
                @DisplayName("Should throw exception when group is disabled")
                void testChangeLeaderDisabledGroup() {
                        Group disabledGroup = Group.builder()
                                        .id(UUID.randomUUID())
                                        .groupName("Disabled")
                                        .bio("Disabled")
                                        .leader(user)
                                        .members(new HashSet<>())
                                        .enable(false)
                                        .build();

                        when(groupRepository.findById(disabledGroup.getId())).thenReturn(Optional.of(disabledGroup));

                        assertThatThrownBy(() -> groupService.changeLeader(disabledGroup.getId(), otherUser.getId(),
                                        user.getId()))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Group is Block");
                }

                @Test
                @DisplayName("Should throw exception when new leader not found")
                void testChangeLeaderNewLeaderNotFound() {
                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.of(group1));
                        when(usersRepository.findByIdAndEnableTrue(otherUser.getId())).thenReturn(null);

                        assertThatThrownBy(() -> groupService.changeLeader(group1.getId(), otherUser.getId(),
                                        user.getId()))
                                        .isInstanceOf(UserNotFoundException.class);
                }

                @Test
                @DisplayName("Should throw exception when new leader not a member")
                void testChangeLeaderNotMember() {
                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.of(group1));
                        when(usersRepository.findByIdAndEnableTrue(otherUser.getId())).thenReturn(otherUser);
                        when(groupMemberRepository.findByGroupIdAndUserId(group1.getId(), otherUser.getId()))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> groupService.changeLeader(group1.getId(), otherUser.getId(),
                                        user.getId()))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("New leader must be a member of the group");
                }

                @Test
                @DisplayName("Should throw exception when parameters are null")
                void testChangeLeaderNullParameters() {
                        assertThatThrownBy(() -> groupService.changeLeader(null, otherUser.getId(), user.getId()))
                                        .isInstanceOf(IllegalArgumentException.class);

                        assertThatThrownBy(() -> groupService.changeLeader(group1.getId(), null, user.getId()))
                                        .isInstanceOf(IllegalArgumentException.class);

                        assertThatThrownBy(() -> groupService.changeLeader(group1.getId(), otherUser.getId(), null))
                                        .isInstanceOf(IllegalArgumentException.class);
                }
        }

        @Nested
        @DisplayName("When deleting group")
        class DeleteGroupTest {

                @Test
                @DisplayName("Should delete group successfully")
                void testDeleteGroupSuccess() {
                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.of(group1));
                        when(groupRepository.save(any(Group.class))).thenReturn(group1);

                        groupService.deleteGroup(group1.getId(), user.getId());

                        assertFalse(group1.getEnable());
                        verify(groupRepository).save(group1);
                        verify(redisService).delete(RedisMethod.GROUP_ + group1.getId().toString());
                }

                @Test
                @DisplayName("Should throw exception when group not found")
                void testDeleteGroupNotFound() {
                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.empty());

                        assertThatThrownBy(() -> groupService.deleteGroup(group1.getId(), user.getId()))
                                        .isInstanceOf(GroupNotFoundException.class);
                }

                @Test
                @DisplayName("Should throw exception when not leader")
                void testDeleteGroupNotLeader() {
                        when(groupRepository.findById(group1.getId())).thenReturn(Optional.of(group1));

                        assertThatThrownBy(() -> groupService.deleteGroup(group1.getId(), otherUser.getId()))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("Only the leader can delete the group");
                }

                @Test
                @DisplayName("Should throw exception when parameters are null")
                void testDeleteGroupNullParameters() {
                        assertThatThrownBy(() -> groupService.deleteGroup(null, user.getId()))
                                        .isInstanceOf(IllegalArgumentException.class);

                        assertThatThrownBy(() -> groupService.deleteGroup(group1.getId(), null))
                                        .isInstanceOf(IllegalArgumentException.class);
                }
        }

        @Nested
        @DisplayName("Integration Tests")
        class IntegrationTests {

                @Test
                @DisplayName("Should create, update, and delete group workflow")
                void testCompleteGroupWorkflow() {
                        when(usersRepository.findByIdAndEnableTrue(user.getId())).thenReturn(user);
                        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> {
                                Group group = invocation.getArgument(0);
                                if (group.getId() == null) {
                                        group.setId(UUID.randomUUID());
                                }
                                return group;
                        });

                        GroupResponseDto createdGroup = groupService.createGroup(createGroupRequestDto, null, null);
                        assertNotNull(createdGroup);
                        assertEquals(1, createdGroup.getMemberCount());

                        verify(groupRepository).save(any(Group.class));
                        verify(kafkaService).saveIntoGroupDatabase(any(GroupResponseDto.class));
                }
        }
}