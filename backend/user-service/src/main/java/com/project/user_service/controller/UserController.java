package com.project.user_service.controller;

import com.project.user_service.domain.dto.request.CreateUserDetailsRequestDto;
import com.project.user_service.domain.dto.request.UpdateUserProfileRequestDto;
import com.project.user_service.domain.dto.response.GetFollowResponse;
import com.project.user_service.domain.dto.response.UserProfileResponseDto;
import com.project.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @PreAuthorize("authentication.principal.id.equals(#requestDto.id)")
    @PostMapping("/profile/create")
    public ResponseEntity<UserProfileResponseDto> createUser(
            @Valid @RequestPart CreateUserDetailsRequestDto requestDto,
            @RequestPart MultipartFile file) {
        UserProfileResponseDto responseDto = userService.createUser(requestDto, file);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @GetMapping("/profile/get")
    public ResponseEntity<UserProfileResponseDto> getUser(
            @RequestParam UUID id, @RequestParam Boolean image) {
        UserProfileResponseDto responseDto = userService.getProfile(id, image);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @PutMapping("/profile/image/update")
    @PreAuthorize("authentication.principal.id.equals(#id)")
    public ResponseEntity<Boolean> uploadImage(
            @RequestPart MultipartFile file, @RequestParam UUID id) {
        return ResponseEntity.ok().body(userService.uploadImage(id, file));
    }

    @PutMapping("/profile/update")
    @PreAuthorize("authentication.principal.id.equals(#id)")
    public ResponseEntity<List<String>> updateProfile(
            @RequestParam UUID id,
            @Valid @RequestBody UpdateUserProfileRequestDto requestDto) {
        List<String> updated = userService.updateUserProfile(id, requestDto);
        return ResponseEntity.ok().body(updated);
    }

    @PostMapping("/{userId}/follow/{targetId}")
    @PreAuthorize("authentication.principal.id.equals(#userId) OR hasRole('MODERATOR')")
    public ResponseEntity<Void> followUser(
            @PathVariable UUID userId,
            @PathVariable UUID targetId) {
        userService.followUser(userId, targetId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/follow/{targetId}")
    @PreAuthorize("authentication.principal.id.equals(#userId) OR hasRole('MODERATOR')")
    public ResponseEntity<Void> unfollowUser(
            @PathVariable UUID userId,
            @PathVariable UUID targetId) {
        userService.unfollowUser(userId, targetId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('user:get')")
    @GetMapping("/following/list")
    public ResponseEntity<Page<GetFollowResponse>> getFollowing(
            @RequestParam UUID id, @RequestParam int page) {
        Page<GetFollowResponse> responseDto = userService.getFollowing(id, page);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('user:get')")
    @GetMapping("/follower/list")
    public ResponseEntity<Page<GetFollowResponse>> getFollower(
            @RequestParam UUID id, @RequestParam int page) {
        Page<GetFollowResponse> responseDto = userService.getFollower(id, page);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }
}
