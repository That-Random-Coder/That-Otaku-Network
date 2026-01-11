package com.project.user_service.controller;

import com.project.user_service.domain.dto.request.BioUpdateGroupRequestDto;
import com.project.user_service.domain.dto.request.CreateGroupRequestDto;
import com.project.user_service.domain.dto.response.GetGroups;
import com.project.user_service.domain.dto.response.GroupResponseDto;
import com.project.user_service.service.GroupService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/group")
public class GroupController {

    private final GroupService groupService;

    @PreAuthorize("authentication.principal.id.equals(#requestDto.leaderId)")
    @PostMapping("/create")
    public ResponseEntity<GroupResponseDto> createGroup(
            @Valid @RequestPart CreateGroupRequestDto requestDto,
            @RequestPart(required = false) MultipartFile profileImage,
            @RequestPart(required = false) MultipartFile bgImage) {
        GroupResponseDto responseDto = groupService.createGroup(requestDto, profileImage, bgImage);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @GetMapping("/get")
    public ResponseEntity<GroupResponseDto> getGroup(
            @RequestParam UUID id,
            @RequestParam(defaultValue = "false") Boolean image) {
        GroupResponseDto responseDto = groupService.getGroup(id, image);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @PutMapping("/image/update")
    @PreAuthorize("hasRole('MODERATOR') OR @groupService.isLeader(authentication.principal.id, #id)")
    public ResponseEntity<List<String>> updateImage(
            @RequestParam UUID id,
            @RequestPart(required = false) MultipartFile profileImage,
            @RequestPart(required = false) MultipartFile bgImage) {
        List<String> updated = groupService.updateImage(id, profileImage, bgImage);
        return ResponseEntity.ok().body(updated);
    }

    @PutMapping("/bio/update")
    @PreAuthorize("hasRole('MODERATOR') OR @groupService.isLeader(authentication.principal.id, #requestDto.id)")
    public ResponseEntity<List<String>> updateBio(
            @Valid @RequestBody BioUpdateGroupRequestDto requestDto) {
        List<String> updated = groupService.updateBio(requestDto);
        return ResponseEntity.ok().body(updated);
    }

    @PutMapping("/leader/change")
    @PreAuthorize("@groupService.isLeader(authentication.principal.id, #groupId)")
    public ResponseEntity<GroupResponseDto> changeLeader(
            @RequestParam UUID groupId,
            @RequestParam UUID newLeaderId,
            @RequestParam UUID currentLeaderId) {
        GroupResponseDto responseDto = groupService.changeLeader(groupId, newLeaderId, currentLeaderId);
        return ResponseEntity.ok().body(responseDto);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@groupService.isLeader(authentication.principal.id, #groupId) OR hasRole('MODERATOR')")
    public ResponseEntity<Void> deleteGroup(
            @RequestParam UUID groupId,
            @RequestParam UUID leaderId) {
        groupService.deleteGroup(groupId, leaderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/join")
    @PreAuthorize("authentication.principal.id.equals(#userId)")
    public ResponseEntity<Void> joinGroup(
            @RequestParam UUID groupId,
            @RequestParam UUID userId) {
        groupService.joinGroup(groupId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/leave")
    @PreAuthorize("authentication.principal.id.equals(#userId)")
    public ResponseEntity<Void> leaveGroup(
            @RequestParam UUID groupId,
            @RequestParam UUID userId) {
        groupService.leaveGroup(groupId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<List<GetGroups>> getGroupsByUser(@PathVariable UUID id) {
        return ResponseEntity.ok(groupService.getGroups(id));
    }
//    hello
}
