package com.project.user_service.service;

import com.project.user_service.domain.dto.request.BioUpdateGroupRequestDto;
import com.project.user_service.domain.dto.request.CreateGroupRequestDto;
import com.project.user_service.domain.dto.response.GetGroups;
import com.project.user_service.domain.dto.response.GroupResponseDto;
import com.project.user_service.domain.entity.groups.Group;
import com.project.user_service.domain.entity.groups.GroupMember;
import com.project.user_service.domain.entity.groups.ImageGroup;
import com.project.user_service.domain.entity.users.Users;
import com.project.user_service.domain.enums.RedisMethod;
import com.project.user_service.domain.security.UserDetailCustom;
import com.project.user_service.exception.customException.GroupNotFoundException;
import com.project.user_service.exception.customException.ImageUploadFailedException;
import com.project.user_service.exception.customException.UserNotFoundException;
import com.project.user_service.repository.GroupMemberRepository;
import com.project.user_service.repository.GroupRepository;
import com.project.user_service.repository.ImageGroupRepository;
import com.project.user_service.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UsersRepository usersRepository;
    private final RedisService redisService;
    private final ImageGroupRepository imageGroupRepository;
    private final KafkaService kafkaService;
    private final GroupMemberRepository groupMemberRepository;

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final int PAGE_LIMIT = 10;
    private static final long TIME_REDIS = 600L;
    private static final long TIME_REDIS_MAX = 36000 * 5;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    private void validateProfileImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Profile image is required");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Profile image must be less than 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG, or WEBP images are allowed");
        }
    }

    @Transactional
    public GroupResponseDto createGroup(CreateGroupRequestDto requestDto,
            MultipartFile profileImage,
            MultipartFile bgImage) {

        Users user = usersRepository.findByIdAndEnableTrue(requestDto.getLeaderId());
        if (user == null) {
            throw new UserNotFoundException(requestDto.getLeaderId().toString());
        }

        Group group = Group.builder()
                .groupName(requestDto.getGroupName())
                .bio(requestDto.getGroupBio())
                .leader(user)
                .build();

        byte[] profileImageBytes = null;
        byte[] bgImageBytes = null;
        String profileImageType = null;
        String bgImageType = null;

        try {
            if (profileImage != null && !profileImage.isEmpty()) {
                validateProfileImage(profileImage);
                profileImageBytes = profileImage.getBytes();
                profileImageType = profileImage.getContentType();
            }
        } catch (IOException e) {
            log.error("Image Upload of profile For Group Fail : {}", e.getMessage());
            throw new ImageUploadFailedException("Failed to profile images");
        }

        try {
            if (bgImage != null && !bgImage.isEmpty()) {
                validateProfileImage(bgImage);
                bgImageBytes = bgImage.getBytes();
                bgImageType = bgImage.getContentType();
            }
        } catch (IOException e) {
            log.error("Image Upload of Background For Group Fail : {}", e.getMessage());
            throw new ImageUploadFailedException("Failed to background images");
        }

        // Save group first to generate ID
        group = groupRepository.saveAndFlush(group);

        // Now create ImageGroup with the generated group ID
        ImageGroup imageGroup = ImageGroup.builder()
                .profileImage(profileImageBytes)
                .profileImageType(profileImageType)
                .bgImage(bgImageBytes)
                .bgImageType(bgImageType)
                .build();

        // Set bidirectional relationship - this will set the ID via @MapsId
        imageGroup.setGroup(group);
        group.setImages(imageGroup);

        // Save the ImageGroup explicitly
        imageGroupRepository.save(imageGroup);

        user.getLeaderOfGroup().add(group);

        log.info("Group is created by Id : {} and groupName : {} by leader : {} ({})",
                group.getId().toString(), group.getGroupName(), user.getId().toString(), user.getUsername());

        GroupResponseDto responseDto = GroupResponseDto
                .builder()
                .id(group.getId())
                .groupName(group.getGroupName())
                .groupBio(group.getBio())
                .dateOfCreation(group.getDateOfCreation())
                .memberCount(1) // Leader counts as 1 member
                .leaderUsername(user.getUsername())
                .leaderDisplayName(user.getDisplayName())
                .leaderId(user.getId())
                .profileImage(imageGroup.getProfileImage())
                .profileImageType(imageGroup.getProfileImageType())
                .bgImage(imageGroup.getBgImage())
                .bgImageType(imageGroup.getBgImageType())
                .build();

        kafkaService.saveIntoGroupDatabase(responseDto);

        redisService.set(RedisMethod.GROUP_ + group.getId().toString(), responseDto, TIME_REDIS);
        return responseDto;
    }

    public GroupResponseDto getGroup(UUID id, Boolean giveImage) {
        if (id == null) {
            throw new IllegalArgumentException("Id can't be Null");
        }

        String redisKey = RedisMethod.GROUP_ + id.toString();
        GroupResponseDto groupResponseDto = redisService.get(redisKey, GroupResponseDto.class);
        if (groupResponseDto != null) {

            redisService.set(redisKey, groupResponseDto, TIME_REDIS);

            if (!giveImage || groupResponseDto.getProfileImage() != null) {
                return groupResponseDto;
            }

            ImageGroup imageGroup = imageGroupRepository.findById(id).orElse(null);
            if (imageGroup == null) {
                return groupResponseDto;
            }
            groupResponseDto.setBgImage(imageGroup.getBgImage());
            groupResponseDto.setBgImageType(imageGroup.getBgImageType());
            groupResponseDto.setProfileImage(imageGroup.getProfileImage());
            groupResponseDto.setProfileImageType(imageGroup.getProfileImageType());

            return groupResponseDto;
        }
        
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetailCustom userDetail;
        if (principal instanceof UserDetailCustom) {
            userDetail = (UserDetailCustom) principal;
        } else {
            throw new IllegalStateException("Principal is not of type UserDetailCustom");
        }
        Optional<GroupResponseDto> getResponseOption = groupRepository.getGroupById(id, userDetail.getId());
        if (getResponseOption.isEmpty()) {
            throw new GroupNotFoundException("Group : " + id);
        }

        long time = TIME_REDIS + getResponseOption.get().getMemberCount();
        time = Math.min(time, TIME_REDIS_MAX);
        redisService.set(RedisMethod.GROUP_ + id.toString(), getResponseOption.get(), time);
        if (!giveImage) {
            return getResponseOption.get();
        }

        ImageGroup imageGroup = imageGroupRepository.findById(id).orElse(null);
        if (imageGroup == null) {
            return getResponseOption.get();
        }

        groupResponseDto = getResponseOption.get();
        groupResponseDto.setProfileImageType(imageGroup.getProfileImageType());
        groupResponseDto.setProfileImage(imageGroup.getProfileImage());
        groupResponseDto.setBgImage(imageGroup.getBgImage());
        groupResponseDto.setBgImageType(imageGroup.getBgImageType());

        return groupResponseDto;
    }

    @Transactional
    public List<String> updateImage(UUID id, MultipartFile profileImage, MultipartFile bgImage) {
        if (id == null) {
            log.error("Group can't update the image due to null id");
            throw new GroupNotFoundException("Group Id cannot be null");
        }

        // Check if group is enabled
        Group group = groupRepository.findById(id).orElse(null);
        if (group == null) {
            throw new GroupNotFoundException("Group : " + id);
        }
        if (!group.getEnable()) {
            throw new IllegalArgumentException("Group is Block");
        }

        List<String> imageUpdate = new ArrayList<>();

        ImageGroup imageGroup = groupRepository.getGroupImageById(id);
        if (imageGroup == null) {
            throw new GroupNotFoundException("Image not found for Group : " + id);
        }

        byte[] profileImageBytes = null;
        byte[] bgImageBytes = null;
        String profileImageType = null;
        String bgImageType = null;

        try {
            if (profileImage != null && !profileImage.isEmpty()) {
                validateProfileImage(profileImage);
                profileImageBytes = profileImage.getBytes();
                profileImageType = profileImage.getContentType();

                imageGroup.setProfileImage(profileImageBytes);
                imageGroup.setProfileImageType(profileImageType);
                imageUpdate.add("Profile Image");
            }
        } catch (IOException e) {
            log.error("Image Upload of profile For Group Fail : {}", e.getMessage());
            throw new ImageUploadFailedException("Failed to profile images");
        }

        try {
            if (bgImage != null && !bgImage.isEmpty()) {
                validateProfileImage(bgImage);
                bgImageBytes = bgImage.getBytes();
                bgImageType = bgImage.getContentType();

                imageGroup.setBgImage(bgImageBytes);
                imageGroup.setBgImageType(bgImageType);

                imageUpdate.add("Background Image");
            }
        } catch (IOException e) {
            log.error("Image Upload of Background For Group Fail : {}", e.getMessage());
            throw new ImageUploadFailedException("Failed to background images");
        }

        imageGroupRepository.save(imageGroup);
        redisService.delete(RedisMethod.GROUP_ + id.toString());

        log.info("Group : {} is updated with {}", id.toString(), imageUpdate.toString());
        return imageUpdate;
    }

    @Transactional
    public List<String> updateBio(BioUpdateGroupRequestDto requestDto) {

        if (requestDto.getId() == null || requestDto.getBio() == null) {
            throw new IllegalArgumentException("Request Missing Id or Bio");
        }

        Group group = groupRepository.findById(requestDto.getId()).orElse(null);
        if (group == null) {
            throw new GroupNotFoundException("Group : " + requestDto.getId());
        }
        if (!group.getEnable()) {
            throw new IllegalArgumentException("Group is Block");
        }

        int update = groupRepository.updateTheBio(requestDto.getId(), requestDto.getBio());

        if (update == 0) {
            throw new GroupNotFoundException("Group : " + requestDto.getId().toString());
        }
        redisService.delete(RedisMethod.GROUP_ + requestDto.getId().toString());
        log.info("Group : {} is updated their bio", requestDto.getId().toString());

        kafkaService.updateIntoGroupDatabase(requestDto.getId(), null, requestDto.getBio(), null, null);

        List<String> updated = new ArrayList<>();
        updated.add("Bio");
        return updated;
    }

    public boolean isLeader(UUID userId, UUID groupId) {
        if (userId == null || groupId == null) {
            return false;
        }

        Group group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return false;
        }

        return group.getLeader().getId().equals(userId);
    }

    @Transactional
    public GroupResponseDto changeLeader(UUID groupId, UUID newLeaderId, UUID currentLeaderId) {
        if (groupId == null || newLeaderId == null || currentLeaderId == null) {
            throw new IllegalArgumentException("Group ID, new leader ID, and current leader ID are required");
        }

        if (newLeaderId.equals(currentLeaderId)) {
            throw new IllegalArgumentException("New leader cannot be the same as current leader");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group : " + groupId));

        if (!group.getLeader().getId().equals(currentLeaderId)) {
            throw new IllegalArgumentException("Only the current leader can transfer leadership");
        }

        if (!group.getEnable()) {
            throw new IllegalArgumentException("Group is Block");
        }

        Users newLeader = usersRepository.findByIdAndEnableTrue(newLeaderId);
        if (newLeader == null) {
            throw new UserNotFoundException(newLeaderId.toString());
        }

        Optional<GroupMember> memberCheck = groupMemberRepository.findByGroupIdAndUserId(groupId, newLeaderId);
        if (memberCheck.isEmpty()) {
            throw new IllegalArgumentException("New leader must be a member of the group");
        }

        Users oldLeader = group.getLeader();

        oldLeader.getLeaderOfGroup().remove(group);

        Optional<GroupMember> oldLeaderAsMember = groupMemberRepository.findByGroupIdAndUserId(groupId,
                currentLeaderId);

        if (oldLeaderAsMember.isEmpty()) {
            GroupMember newMember = GroupMember.builder()
                    .users(oldLeader)
                    .group(group)
                    .build();
            group.getMembers().add(newMember);
            groupMemberRepository.save(newMember);
        }

        GroupMember memberToRemove = memberCheck.get();
        group.getMembers().remove(memberToRemove);
        groupMemberRepository.delete(memberToRemove);

        group.setLeader(newLeader);
        newLeader.getLeaderOfGroup().add(group);

        groupRepository.save(group);

        log.info("Group : {} leadership transferred from {} to {}",
                groupId, oldLeader.getUsername(), newLeader.getUsername());

        redisService.delete(RedisMethod.GROUP_ + groupId.toString());

        GroupResponseDto responseDto = GroupResponseDto.builder()
                .id(group.getId())
                .groupName(group.getGroupName())
                .groupBio(group.getBio())
                .dateOfCreation(group.getDateOfCreation())
                .memberCount(group.getMembers().size() + 1) // Members + leader
                .leaderUsername(newLeader.getUsername())
                .leaderDisplayName(newLeader.getDisplayName())
                .leaderId(newLeader.getId())
                .profileImage(null)
                .profileImageType(null)
                .bgImage(null)
                .bgImageType(null)
                .build();

        kafkaService.updateIntoGroupDatabase(groupId, null, null, newLeader.getId().toString(),
                newLeader.getUsername());

        return responseDto;
    }

    @Transactional
    public void deleteGroup(UUID groupId, UUID leaderId) {
        if (groupId == null || leaderId == null) {
            throw new IllegalArgumentException("Group ID and leader ID are required");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group : " + groupId));

        if (!group.getLeader().getId().equals(leaderId)) {
            throw new IllegalArgumentException("Only the leader can delete the group");
        }

        Users leader = group.getLeader();

        leader.getLeaderOfGroup().remove(group);
        group.setEnable(false);
        groupRepository.save(group);
        redisService.delete(RedisMethod.GROUP_ + groupId.toString());

        log.info("Group : {} ({}) has been deleted by leader : {} ({})",
                groupId, group.getGroupName(), leaderId, leader.getUsername());
    }

    @Transactional
    public void joinGroup(UUID groupId, UUID userId) {
        if (groupId == null || userId == null) {
            throw new IllegalArgumentException("Group ID and User ID are required");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group : " + groupId));

        if (!group.getEnable()) {
            throw new IllegalArgumentException("Group is blocked");
        }

        // Check if user is already the leader
        if (group.getLeader().getId().equals(userId)) {
            throw new IllegalArgumentException("Leader cannot join as a member");
        }

        Users user = usersRepository.findByIdAndEnableTrue(userId);
        if (user == null) {
            throw new UserNotFoundException(userId.toString());
        }

        // Check if already a member
        Optional<GroupMember> existingMember = groupMemberRepository.findByGroupIdAndUserId(groupId, userId);
        if (existingMember.isPresent()) {
            throw new IllegalArgumentException("User is already a member of this group");
        }

        GroupMember newMember = GroupMember.builder()
                .users(user)
                .group(group)
                .build();

        group.getMembers().add(newMember);
        groupMemberRepository.save(newMember);

        // Invalidate cache
        redisService.delete(RedisMethod.GROUP_ + groupId.toString());

        log.info("User : {} ({}) joined group : {} ({})",
                userId, user.getUsername(), groupId, group.getGroupName());
    }

    @Transactional
    public void leaveGroup(UUID groupId, UUID userId) {
        if (groupId == null || userId == null) {
            throw new IllegalArgumentException("Group ID and User ID are required");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group : " + groupId));

        // Leader cannot leave, they must transfer leadership or delete the group
        if (group.getLeader().getId().equals(userId)) {
            throw new IllegalArgumentException(
                    "Leader cannot leave the group. Transfer leadership or delete the group.");
        }

        Optional<GroupMember> memberOpt = groupMemberRepository.findByGroupIdAndUserId(groupId, userId);
        if (memberOpt.isEmpty()) {
            throw new IllegalArgumentException("User is not a member of this group");
        }

        GroupMember member = memberOpt.get();
        group.getMembers().remove(member);
        groupMemberRepository.delete(member);

        // Invalidate cache
        redisService.delete(RedisMethod.GROUP_ + groupId.toString());

        log.info("User : {} left group : {} ({})",
                userId, groupId, group.getGroupName());
    }

    public boolean isMember(UUID userId, UUID groupId) {
        if (userId == null || groupId == null) {
            return false;
        }

        // Check if user is the leader
        Group group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return false;
        }

        if (group.getLeader().getId().equals(userId)) {
            return true;
        }

        // Check if user is a member
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId).isPresent();
    }

    public List<GetGroups> getGroups(UUID id) {
        return groupMemberRepository.getGroupOfUser(id);
    }
}
