package com.project.auth_service.repository;

import com.project.auth_service.domain.entity.RefreshToken;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken , UUID> {
    Optional<RefreshToken> findByRefreshTokenAndEnableTrue(String refreshToken);

    RefreshToken findFirstByUserProfileUsernameAndEnableTrue(String username);

    @Modifying
    @Query("""
        UPDATE RefreshToken r
        SET r.enable = false
        WHERE r.userProfile.id = :userId
    """)
    void disableAllByUserId(@Param("userId") UUID userId);

    @Query("""
    SELECT r FROM RefreshToken r
    WHERE r.enable = true AND r.userProfile.id = :userId
    """)
    Optional<RefreshToken> findActiveByUserId(@Param("userId") UUID userId);
}
