package com.project.user_service.repository;

import com.project.user_service.domain.entity.users.ImageUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ImageUserEntityRepository extends JpaRepository<ImageUserEntity , UUID> {
}
