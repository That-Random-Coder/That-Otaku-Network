package com.project.user_service.service;

import com.project.user_service.domain.dto.request.CreateUserDetailsRequestDto;
import com.project.user_service.domain.dto.request.UpdateUserProfileRequestDto;
import com.project.user_service.domain.dto.response.GetFollowResponse;
import com.project.user_service.domain.dto.response.UserProfileResponseDto;
import com.project.user_service.domain.entity.users.Follow;
import com.project.user_service.domain.entity.users.ImageUserEntity;
import com.project.user_service.domain.entity.users.Users;
import com.project.user_service.domain.enums.RedisMethod;
import com.project.user_service.domain.security.UserDetailCustom;
import com.project.user_service.exception.customException.ImageUploadFailedException;
import com.project.user_service.exception.customException.UserAlreadyExistsException;
import com.project.user_service.exception.customException.UserNotFoundException;
import com.project.user_service.repository.FollowRepository;
import com.project.user_service.repository.ImageUserEntityRepository;
import com.project.user_service.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final UsersRepository userRepository;
    private final RedisService redisService;
    private final FollowRepository followRepository;
    private final KafkaService kafkaService;
    private final ImageUserEntityRepository imageUserEntityRepository;

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final int PAGE_LIMIT = 15;
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
    public UserProfileResponseDto createUser(CreateUserDetailsRequestDto requestDto, MultipartFile file) {

        if (userRepository.existsById(requestDto.getId())) {
            throw new UserAlreadyExistsException(requestDto.getUsername());
        }

        byte[] image = null;
        String imageType = null;

        if (file != null && !file.isEmpty()) {

            validateProfileImage(file);

            try {
                image = file.getBytes();
                imageType = file.getContentType();
            } catch (IOException e) {
                throw new ImageUploadFailedException("Failed to profile image");
            }
        }

        ImageUserEntity imageEntity = ImageUserEntity
                .builder()
                .id(requestDto.getId())
                .image(image)
                .imageType(imageType)
                .build();

        Users user = Users
                .builder()
                .id(requestDto.getId())
                .username(requestDto.getUsername())
                .bio(requestDto.getBio())
                .enable(true)
                .location(requestDto.getLocation())
                .displayName(requestDto.getDisplayName())
                .dateOfBirth(requestDto.getDateOfBirth())
                .imageUserEntity(imageEntity)
                .build();

        userRepository.save(user);

        log.info("User {} created with id {}", user.getUsername(), user.getId());

        UserProfileResponseDto responseDto = UserProfileResponseDto
                .builder()
                .id(requestDto.getId())
                .displayName(requestDto.getDisplayName())
                .username(requestDto.getUsername())
                .bio(user.getBio())
                .location(user.getLocation())
                .profileImg(user.getImageUserEntity().getImage())
                .imageType(user.getImageUserEntity().getImageType())
                .isVerified(false)
                .followers(0)
                .following(0)
                .build();
        responseDto.setIsFollow(false);

        redisService.set(RedisMethod.USER_ + user.getId().toString(), responseDto, TIME_REDIS);
        kafkaService.saveIntoUserDatabase(user);

        return responseDto;
    }

    @Transactional
    public UserProfileResponseDto getProfile(UUID id, Boolean image) {

        UserProfileResponseDto responseDto = redisService.get(RedisMethod.USER_ + id.toString(),
                UserProfileResponseDto.class);

        if (responseDto != null) {
            long time = TIME_REDIS + responseDto.getFollowers();
            time = Math.min(time, TIME_REDIS_MAX);
            redisService.set(RedisMethod.USER_ + id.toString(), responseDto, time);
            return responseDto;
        }

        Optional<UserProfileResponseDto> optUser = userRepository.getUserWithDetails(id);
        if (optUser.isEmpty()) {
            throw new UserNotFoundException(id.toString());
        }

        byte[] imagebytes = null;
        String imageType = null;
        if (image) {
            ImageUserEntity imageUserEntity = imageUserEntityRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException(id.toString()));

            imagebytes = imageUserEntity.getImage();
            imageType = imageUserEntity.getImageType();
        }
        responseDto = optUser.get();
        responseDto.setImageType(imageType);
        responseDto.setProfileImg(imagebytes);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Boolean following = false;

        if (authentication != null) {
            UserDetailCustom userDetailCustom = (UserDetailCustom) authentication.getPrincipal();

            following = followRepository.existsByFollower_IdAndFollowing_Id(userDetailCustom.getId(), id);
        }

        responseDto.setIsFollow(following);

        long time = TIME_REDIS + optUser.get().getFollowers();
        time = Math.min(time, TIME_REDIS_MAX);
        redisService.set(RedisMethod.USER_ + optUser.get().getId().toString(), responseDto, time);
        return responseDto;

    }

    @Transactional
    public boolean uploadImage(UUID id, MultipartFile file) {
        if (id == null || file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Id or Image");
        }

        validateProfileImage(file);

        if (!userRepository.existsByIdAndEnableTrue(id)) {
            throw new UserNotFoundException("User : " + id.toString());
        }

        try {
            ImageUserEntity imageUserEntity = imageUserEntityRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("Image " + id.toString()));

            imageUserEntity.setImage(file.getBytes());
            imageUserEntity.setImageType(file.getContentType());

            imageUserEntityRepository.save(imageUserEntity);

            redisService.delete(RedisMethod.USER_ + id.toString());

        } catch (IOException e) {
            throw new ImageUploadFailedException(e.getMessage());
        }

        log.info("User : {} update Profile Image", id.toString());
        return true;
    }

    @Transactional
    public List<String> updateUserProfile(
            UUID id,
            UpdateUserProfileRequestDto dto) {

        if (id == null) {
            throw new IllegalArgumentException("User id must not be null");
        }

        List<String> updated = new ArrayList<>();
        String displayName = null;
        String bio = null;

        Users user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));

        if (!user.isEnable()) {
            throw new UserNotFoundException(id.toString());
        }

        if (dto.getDisplayName() != null) {
            user.setDisplayName(dto.getDisplayName());
            displayName = user.getDisplayName();
            updated.add("Display Name");
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
            bio = user.getBio();
            updated.add("Bio");
        }

        if (dto.getLocation() != null) {
            user.setLocation(dto.getLocation());
            updated.add("Location");
        }

        if (dto.getDateOfBirth() != null) {
            user.setDateOfBirth(dto.getDateOfBirth());
            updated.add("Date of Birth");
        }

        userRepository.save(user);

        log.info("User : {} ({}) updated its : {}", user.getId().toString(), user.getUsername(), updated.toString());

        redisService.delete(RedisMethod.USER_ + user.getId().toString());

        if (displayName != null || bio != null) {
            kafkaService.updateIntoUserDatabase(user.getId(), displayName, bio, user.getUsername());
        }

        return updated;
    }

    @Transactional
    public void followUser(UUID userId, UUID targetUserId) {

        if (userId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot follow yourself");
        }

        Users user = userRepository.findByIdAndEnableTrue(userId);

        if (user == null) {
            throw new UserNotFoundException(userId.toString());
        }

        Users targetUser = userRepository.findByIdAndEnableTrue(targetUserId);

        if (targetUser == null) {
            throw new UserNotFoundException(userId.toString());
        }

        if (followRepository.existsByFollowerAndFollowing(user, targetUser)) {
            throw new IllegalStateException("Already following this user");
        }

        followRepository.save(
                Follow.builder()
                        .follower(user)
                        .following(targetUser)
                        .build());

        redisService.delete(RedisMethod.USER_ + userId.toString());
        redisService.delete(RedisMethod.USER_ + targetUserId.toString());

        log.info("User {} followed {}", userId, targetUserId);
    }

    @Transactional
    public void unfollowUser(UUID userId, UUID targetUserId) {

        Users user = userRepository.findByIdAndEnableTrue(userId);

        if (user == null) {
            throw new UserNotFoundException(userId.toString());
        }

        Users targetUser = userRepository.findByIdAndEnableTrue(targetUserId);

        if (targetUser == null) {
            throw new UserNotFoundException(userId.toString());
        }

        int deleted = followRepository.deleteFollowerAndFollowing(user, targetUser);

        if (deleted == 0) {
            throw new IllegalStateException("You are not following this user");
        }

        redisService.delete(RedisMethod.USER_ + userId.toString());
        redisService.delete(RedisMethod.USER_ + targetUserId.toString());

        log.info("User {} unfollowed {}", userId, targetUserId);
    }

    @Transactional
    public Page<GetFollowResponse> getFollowing(UUID id, int page) {
        if (id == null) {
            throw new IllegalArgumentException("ID");
        }

        if (page < 0) {
            page = 0;
        }

        Pageable pageable = PageRequest.of(page, PAGE_LIMIT);

        return followRepository.getFollowing(id, pageable);
    }

    @Transactional
    public Page<GetFollowResponse> getFollower(UUID id, int page) {
        if (id == null) {
            throw new IllegalArgumentException("ID");
        }

        if (page < 0) {
            page = 0;
        }

        Pageable pageable = PageRequest.of(page, PAGE_LIMIT);

        return followRepository.getFollower(id, pageable);
    }

}
