package com.project.user_service.repository;

import com.project.user_service.domain.dto.response.UserProfileResponseDto;
import com.project.user_service.domain.entity.users.Follow;
import com.project.user_service.domain.entity.users.Users;
import com.project.user_service.repository.UsersRepository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UsersRepositoryTest {

        @Autowired
        private UsersRepository usersRepository;

        @Autowired
        private FollowRepository followRepository;

        @Autowired
        private EntityManager entityManager;

        private Users testUser;
        private Users follower1;
        private Users follower2;

        @BeforeEach
        @Transactional
        void setUp() {
                testUser = Users.builder()
                                .id(UUID.randomUUID())
                                .username("testuser")
                                .displayName("Test User")
                                .bio("Test bio")
                                .location("Test Location")
                                .dateOfBirth(LocalDate.of(2000, 1, 1))
                                .enable(true)
                                .isVerified(false)
                                .createdAt(LocalDateTime.now())
                                .build();
                entityManager.persist(testUser);

                follower1 = Users.builder()
                                .id(UUID.randomUUID())
                                .username("follower1")
                                .displayName("Follower One")
                                .bio("Follower 1 bio")
                                .location("Location 1")
                                .dateOfBirth(LocalDate.of(1998, 5, 10))
                                .enable(true)
                                .isVerified(true)
                                .createdAt(LocalDateTime.now())
                                .build();
                entityManager.persist(follower1);

                follower2 = Users.builder()
                                .id(UUID.randomUUID())
                                .username("follower2")
                                .displayName("Follower Two")
                                .bio("Follower 2 bio")
                                .location("Location 2")
                                .dateOfBirth(LocalDate.of(1999, 8, 15))
                                .enable(true)
                                .isVerified(false)
                                .createdAt(LocalDateTime.now())
                                .build();
                entityManager.persist(follower2);

                entityManager.flush();
        }

        @AfterEach
        @Transactional
        void tearDown() {
                followRepository.deleteAll();
                usersRepository.deleteAll();
        }

        @Test
        @Transactional
        @DisplayName("Should find user by id when user exists")
        void testFindById_WhenUserExists() {
                Optional<Users> foundUser = usersRepository.findById(testUser.getId());

                assertThat(foundUser).isPresent();
                assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
                assertThat(foundUser.get().getDisplayName()).isEqualTo("Test User");
                assertThat(foundUser.get().getBio()).isEqualTo("Test bio");
        }

        @Test
        @Transactional
        @DisplayName("Should return empty optional when user does not exist")
        void testFindById_WhenUserDoesNotExist() {
                UUID nonExistentId = UUID.randomUUID();

                Optional<Users> foundUser = usersRepository.findById(nonExistentId);

                assertThat(foundUser).isEmpty();
        }

        @Test
        @Transactional
        @DisplayName("Should check if user exists by id")
        void testExistsById() {
                boolean exists = usersRepository.existsById(testUser.getId());
                boolean notExists = usersRepository.existsById(UUID.randomUUID());

                assertThat(exists).isTrue();
                assertThat(notExists).isFalse();
        }

        @Test
        @Transactional
        @DisplayName("Should find enabled user by id")
        void testFindByIdAndEnableTrue() {
                Users enabledUser = usersRepository.findByIdAndEnableTrue(testUser.getId());

                assertThat(enabledUser).isNotNull();
                assertThat(enabledUser.getUsername()).isEqualTo("testuser");
        }

        @Test
        @Transactional
        @DisplayName("Should find user by id with followers and following")
        void testFindByIdWithFollowersAndFollowing() {
                Follow follow1 = Follow.builder()
                                .follower(follower1)
                                .following(testUser)
                                .build();
                followRepository.save(follow1);

                Follow follow2 = Follow.builder()
                                .follower(follower2)
                                .following(testUser)
                                .build();
                followRepository.save(follow2);

                Follow follow3 = Follow.builder()
                                .follower(testUser)
                                .following(follower1)
                                .build();
                followRepository.save(follow3);

                entityManager.flush();
                entityManager.clear();

                Optional<Users> foundUser = usersRepository.findByIdWithFollowersAndFollowing(testUser.getId());

                assertThat(foundUser).isPresent();
                assertThat(foundUser.get().getFollowers()).hasSize(2);
                assertThat(foundUser.get().getFollowing()).hasSize(1);
        }

        @Test
        @Transactional
        @DisplayName("Should find user by id with followers")
        void testFindByIdWithFollowers() {
                Follow follow1 = Follow.builder()
                                .follower(follower1)
                                .following(testUser)
                                .build();
                followRepository.save(follow1);

                Follow follow2 = Follow.builder()
                                .follower(follower2)
                                .following(testUser)
                                .build();
                followRepository.save(follow2);

                entityManager.flush();
                entityManager.clear();

                Optional<Users> foundUser = usersRepository.findByIdWithFollowers(testUser.getId());

                assertThat(foundUser).isPresent();
                assertThat(foundUser.get().getFollowers()).hasSize(2);
                assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
        }

        @Test
        @Transactional
        @DisplayName("Should find enabled user by id with followers")
        void testFindByIdAndEnableTrueWithFollowers() {
                Optional<Users> foundUser = usersRepository.findByIdAndEnableTrueWithFollowers(testUser.getId());

                assertThat(foundUser).isPresent();
                assertThat(foundUser.get().isEnable()).isTrue();
                assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
        }

        @Test
        @Transactional
        @DisplayName("Should get user profile with details")
        void testGetUserWithDetails() {
                Follow follow1 = Follow.builder()
                                .follower(follower1)
                                .following(testUser)
                                .build();
                followRepository.save(follow1);

                Follow follow2 = Follow.builder()
                                .follower(follower2)
                                .following(testUser)
                                .build();
                followRepository.save(follow2);

                Follow follow3 = Follow.builder()
                                .follower(testUser)
                                .following(follower1)
                                .build();
                followRepository.save(follow3);

                entityManager.flush();
                entityManager.clear();

                Optional<UserProfileResponseDto> profile = usersRepository.getUserWithDetails(testUser.getId());

                assertThat(profile).isPresent();
                assertThat(profile.get().getUsername()).isEqualTo("testuser");
                assertThat(profile.get().getDisplayName()).isEqualTo("Test User");
                assertThat(profile.get().getBio()).isEqualTo("Test bio");
                assertThat(profile.get().getFollowers()).isEqualTo(2L);
                assertThat(profile.get().getFollowing()).isEqualTo(1L);
                assertThat(profile.get().getIsVerified()).isFalse();
        }

        @Test
        @Transactional
        @DisplayName("Should check if enabled user exists by id")
        void testExistsByIdAndEnableTrue() {
                boolean exists = usersRepository.existsByIdAndEnableTrue(testUser.getId());

                assertThat(exists).isTrue();
        }

        @Test
        @Transactional
        @DisplayName("Should save and retrieve user successfully")
        void testSaveUser() {
                Users newUser = Users.builder()
                                .id(UUID.randomUUID())
                                .username("newuser")
                                .displayName("New User")
                                .bio("New user bio")
                                .location("New Location")
                                .dateOfBirth(LocalDate.of(1995, 3, 20))
                                .enable(true)
                                .isVerified(true)
                                .createdAt(LocalDateTime.now())
                                .build();

                Users savedUser = usersRepository.save(newUser);
                entityManager.flush();
                entityManager.detach(savedUser);

                Optional<Users> retrievedUser = usersRepository.findById(savedUser.getId());

                assertThat(retrievedUser).isPresent();
                assertThat(retrievedUser.get().getUsername()).isEqualTo("newuser");
        }
}
