package com.project.user_service.repository;

import com.project.user_service.domain.dto.response.GetFollowResponse;
import com.project.user_service.domain.entity.users.Follow;
import com.project.user_service.domain.entity.users.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {

    boolean existsByFollowerAndFollowing(Users follower, Users following);

    long countByFollower(Users follower);

    long countByFollowing(Users following);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.follower = :follower AND f.following = :following")
    int deleteFollowerAndFollowing(
            @Param("follower") Users follower,
            @Param("following") Users following);

    @Query("""
    SELECT new com.project.user_service.domain.dto.response.GetFollowResponse(
        f.following.id,
        f.following.displayName,
        f.following.username,
        f.following.isVerified
    )
    FROM Follow f
    WHERE f.follower.id = :followerId
""")
    Page<GetFollowResponse> getFollowing(@Param("followerId") UUID followerId , Pageable pageable);

    @Query("""
    SELECT new com.project.user_service.domain.dto.response.GetFollowResponse(
        f.follower.id,
        f.follower.displayName,
        f.follower.username,
        f.follower.isVerified
    )
    FROM Follow f
    WHERE f.following.id = :id
""")
    Page<GetFollowResponse> getFollower(@Param("id") UUID id , Pageable pageable);

    @Query(
            "SELECT f.following.id " +
            "FROM Follow f "+
            "WHERE f.follower.id = :id"
    )
    Set<UUID> getFollowingForUser(@Param("id")UUID id);

    Boolean existsByFollower_IdAndFollowing_Id(UUID id, UUID id1);
}
