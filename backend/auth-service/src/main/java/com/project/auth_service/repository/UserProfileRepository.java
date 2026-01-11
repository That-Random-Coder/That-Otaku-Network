package com.project.auth_service.repository;

import com.project.auth_service.domain.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile , UUID> {

    UserProfile findByUsernameOrEmail(String username, String email);

}
