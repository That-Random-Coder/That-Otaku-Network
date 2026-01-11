package com.project.user_service.repository;

import com.project.user_service.domain.entity.groups.Group;
import com.project.user_service.domain.entity.groups.ImageGroup;
import com.project.user_service.domain.entity.users.Users;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GroupRepositoryGetImageByIdTest {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("getGroupImageById returns the group's ImageGroup")
    void getGroupImageById_returnsImage() {

        Users leader = Users.builder()
                .id(UUID.randomUUID())
                .username("leaderUser")
                .displayName("Leader User")
                .bio("Leader bio")
                .location("Earth")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .enable(true)
                .isVerified(false)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        entityManager.persist(leader);

        // Create an ImageGroup
        byte[] profileBytes = new byte[] { 1, 2, 3 };
        byte[] bgBytes = new byte[] { 4, 5, 6 };
        ImageGroup images = ImageGroup.builder()
                .id(UUID.randomUUID())
                .profileImage(profileBytes)
                .profileImageType("image/png")
                .bgImage(bgBytes)
                .bgImageType("image/jpeg")
                .build();

        Group group = Group.builder()
                .groupName("test-group")
                .bio("test bio")
                .leader(leader)
                .images(images)
                .build();
        entityManager.persist(group);
        entityManager.flush();

        ImageGroup found = groupRepository.getGroupImageById(group.getId());

        assertNotNull(found, "ImageGroup should not be null");
        assertArrayEquals(profileBytes, found.getProfileImage(), "profile image bytes should match");
        assertEquals("image/png", found.getProfileImageType());
        assertArrayEquals(bgBytes, found.getBgImage(), "bg image bytes should match");
        assertEquals("image/jpeg", found.getBgImageType());
    }
}
