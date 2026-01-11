package com.project.content_service.service;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import recommendation.RecommendationGrpc;

import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GrpcServices {
        private final recommendation.RecommendationGrpc.RecommendationBlockingStub blockingStub;

        public GrpcServices(
                        @Value("${user.service.address}") String address, @Value("${user.service.port}") int port) {
                ManagedChannel channel = ManagedChannelBuilder
                                .forAddress(address, port)
                                .usePlaintext()
                                .build();

                blockingStub = RecommendationGrpc.newBlockingStub(channel);
        }

        public List<UUID> getUserFollowing(UUID id) {
                recommendation.GetUserFollowingUserId request = recommendation.GetUserFollowingUserId
                                .newBuilder()
                                .setUserId(id.toString())
                                .build();

                recommendation.GetUserIds response = blockingStub.getUserFollowingByUserId(request);

                return response.getUserIdsList()
                                .stream()
                                .map(UUID::fromString)
                                .collect(Collectors.toList());
        }

        public List<UUID> getUserGroupMembersByGroupId(UUID groupId) {
                recommendation.GetUserGroupMemberByGroupId request = recommendation.GetUserGroupMemberByGroupId
                                .newBuilder()
                                .setGroupId(groupId.toString())
                                .build();

                recommendation.GetUserIds response = blockingStub.getUserGroupMembersByGroupId(request);

                return response.getUserIdsList()
                                .stream()
                                .map(UUID::fromString)
                                .collect(Collectors.toList());
        }

        public List<UUID> getUserAllGroupsMembersByUserId(UUID userId) {
                recommendation.GetUserGroupMemberByUserId request = recommendation.GetUserGroupMemberByUserId
                                .newBuilder()
                                .setUserId(userId.toString())
                                .build();

                recommendation.GetUserIds response = blockingStub.getUserAllGroupsMembersByUserId(request);

                System.out.println(response.getUserIdsList().toString());

                return response.getUserIdsList()
                                .stream()
                                .map(UUID::fromString)
                                .collect(Collectors.toList());
        }

}