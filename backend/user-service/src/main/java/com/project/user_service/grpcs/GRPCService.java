package com.project.user_service.grpcs;

import com.project.user_service.domain.entity.users.Follow;
import com.project.user_service.repository.FollowRepository;
import com.project.user_service.repository.GroupMemberRepository;
import com.project.user_service.repository.GroupRepository;
import com.project.user_service.repository.UsersRepository;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;
import recommendation.GetUserGroupMemberByGroupId;
import recommendation.GetUserGroupMemberByUserId;
import recommendation.GetUserIds;
import recommendation.RecommendationGrpc;

import java.util.Set;
import java.util.UUID;

@GrpcService
@AllArgsConstructor
public class GRPCService extends RecommendationGrpc.RecommendationImplBase {

    private final FollowRepository followRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UsersRepository usersRepository;
    private final GroupRepository groupRepository;

    @Override
    public void getUserGroupMembersByGroupId(GetUserGroupMemberByGroupId request,
            StreamObserver<GetUserIds> responseObserver) {
        String groupIdString = request.getGroupId();
        UUID groupId = UUID.fromString(groupIdString);

        Set<UUID> groupIds = groupMemberRepository.getAllMemberOfGroup(groupId);
        groupIds.add(groupRepository.getGroupLeaderId(groupId));
        GetUserIds groupIdResponse = GetUserIds.newBuilder()
                .addAllUserIds(
                        groupIds.stream()
                                .map(UUID::toString)
                                .toList()
                )
                .build();

        responseObserver.onNext(groupIdResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void getUserAllGroupsMembersByUserId(GetUserGroupMemberByUserId request,
            StreamObserver<GetUserIds> responseObserver) {

        String userIdString = request.getUserId();
        UUID userId = UUID.fromString(userIdString);

        Set<UUID> groupIds = groupMemberRepository.getUserGroup(userId);
        Set<UUID> leaderIds = groupMemberRepository.getAllGroupLeaderId(userId);
        Set<UUID> userIds = groupMemberRepository.getGroupMemberByGroupId(groupIds);
        userIds.addAll(leaderIds);

        GetUserIds userIdResponse = GetUserIds.newBuilder()
                .addAllUserIds(
                        userIds.stream()
                                .map(UUID::toString)
                                .toList()
                )
                .build();

        responseObserver.onNext(userIdResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void getUserFollowingByUserId(recommendation.GetUserFollowingUserId request,
            StreamObserver<GetUserIds> responseObserver) {

        String userIdString = request.getUserId();
        UUID userId = UUID.fromString(userIdString);

        Set<UUID> following = followRepository.getFollowingForUser(userId);
        GetUserIds userIds = GetUserIds.newBuilder()
                .addAllUserIds(
                        following.stream()
                                .map(UUID::toString)
                                .toList()
                )
                .build();

        responseObserver.onNext(userIds);
        responseObserver.onCompleted();

    }
}
