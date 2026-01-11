package com.project.content_service.controller;

import com.project.content_service.service.GrpcServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/grpc/user")
@RequiredArgsConstructor
public class GrpcUserController {
    private final GrpcServices grpcServices;

    @GetMapping("/following")
    public ResponseEntity<List<UUID>> getUserFollowing(@RequestParam UUID userId) {
        return ResponseEntity.ok(grpcServices.getUserFollowing(userId));
    }

    @GetMapping("/group-members")
    public ResponseEntity<List<UUID>> getUserGroupMembersByGroupId(@RequestParam UUID groupId) {
        return ResponseEntity.ok(grpcServices.getUserGroupMembersByGroupId(groupId));
    }

    @GetMapping("/all-groups-members")
    public ResponseEntity<List<UUID>> getUserAllGroupsMembersByUserId(@RequestParam UUID userId) {
        return ResponseEntity.ok(grpcServices.getUserAllGroupsMembersByUserId(userId));
    }
}
