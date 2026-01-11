package com.project.user_service.repository;

import com.project.user_service.domain.dto.response.UserProfileResponseDto;
import com.project.user_service.domain.entity.users.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UsersRepository extends JpaRepository<Users, UUID> {

    @Query("SELECT u FROM Users u LEFT JOIN FETCH u.followers LEFT JOIN FETCH u.following WHERE u.id = :id")
    Optional<Users> findByIdWithFollowers(@Param("id") UUID id);

    boolean existsById(UUID id);

    Users findByIdAndEnableTrue(UUID id);

    @Query("SELECT u FROM Users u WHERE u.id = :id AND u.enable = true")
    Optional<Users> findByIdAndEnableTrueWithFollowers(@Param("id") UUID id);

    @Query("""
        SELECT u FROM Users u
        LEFT JOIN FETCH u.followers
        LEFT JOIN FETCH u.following
        WHERE u.id = :id
    """)
    Optional<Users> findByIdWithFollowersAndFollowing(UUID id);


    @Query("""
    SELECT new com.project.user_service.domain.dto.response.UserProfileResponseDto(
        u.id,
        u.username,
        u.displayName,
        u.bio,
        (SELECT COUNT(f) FROM Follow f WHERE f.following.id = u.id),
        (SELECT COUNT(f) FROM Follow f WHERE f.follower.id = u.id),
        u.location,
        null,
        null,
        u.isVerified,
        null
    )
    FROM Users u
    WHERE u.id = :id AND u.enable = true
""")
    Optional<UserProfileResponseDto> getUserWithDetails(UUID id);

    boolean existsByIdAndEnableTrue(UUID id);
}
