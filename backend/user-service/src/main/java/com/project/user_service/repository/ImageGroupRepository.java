package com.project.user_service.repository;

import com.project.user_service.domain.entity.groups.ImageGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageGroupRepository extends JpaRepository<ImageGroup, UUID> {
}
