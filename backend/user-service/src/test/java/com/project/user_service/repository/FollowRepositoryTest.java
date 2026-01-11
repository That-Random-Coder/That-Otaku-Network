package com.project.user_service.repository;

import com.project.user_service.domain.dto.response.GetFollowResponse;
import com.project.user_service.domain.entity.users.Follow;
import com.project.user_service.domain.entity.users.Users;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class FollowRepositoryTest {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private EntityManager entityManager;

    private Users user1;
    private Users user2;
    private Users user3;

    @BeforeEach
    void setUp() {
        user1 = Users.builder()
                .id(UUID.randomUUID())
                .username("testuser1")
                .displayName("Test User 1")
                .bio("Test bio 1")
                .location("Test Location")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .enable(true)
                .isVerified(false)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(user1);

        user2 = Users.builder()
                .id(UUID.randomUUID())
                .username("testuser2")
                .displayName("Test User 2")
                .bio("Test bio 2")
                .location("Test Location")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .enable(true)
                .isVerified(false)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(user2);

        user3 = Users.builder()
                .id(UUID.randomUUID())
                .username("testuser3")
                .displayName("Test User 3")
                .bio("Test bio 3")
                .location("Test Location")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .enable(true)
                .isVerified(false)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(user3);

        entityManager.flush();
    }

    @Test
    @DisplayName("Should save follow relationship successfully")
    void testSaveFollow() {
        Follow follow = Follow.builder()
                .follower(user1)
                .following(user2)
                .build();

        Follow savedFollow = followRepository.save(follow);
        entityManager.flush();

        assertThat(savedFollow).isNotNull();
        assertThat(savedFollow.getId()).isNotNull();
        assertThat(savedFollow.getFollower().getId()).isEqualTo(user1.getId());
        assertThat(savedFollow.getFollowing().getId()).isEqualTo(user2.getId());
    }

    @Test
    @DisplayName("Should check if follow relationship exists")
    void testExistsByFollowerAndFollowing_WhenExists() {
        Follow follow = Follow.builder()
                .follower(user1)
                .following(user2)
                .build();
        followRepository.save(follow);
        entityManager.flush();

        boolean exists = followRepository.existsByFollowerAndFollowing(user1, user2);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when follow relationship does not exist")
    void testExistsByFollowerAndFollowing_WhenNotExists() {
        boolean exists = followRepository.existsByFollowerAndFollowing(user1, user2);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should check if follow relationship exists by IDs")
    void testExistsByFollowerIdAndFollowingId() {
        Follow follow = Follow.builder()
                .follower(user1)
                .following(user2)
                .build();
        followRepository.save(follow);
        entityManager.flush();

        Boolean exists = followRepository.existsByFollower_IdAndFollowing_Id(user1.getId(), user2.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should count followers for a user")
    void testCountByFollowing() {
        Follow follow1 = Follow.builder()
                .follower(user1)
                .following(user3)
                .build();
        Follow follow2 = Follow.builder()
                .follower(user2)
                .following(user3)
                .build();
        followRepository.save(follow1);
        followRepository.save(follow2);
        entityManager.flush();

        long followerCount = followRepository.countByFollowing(user3);

        assertThat(followerCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Should count following for a user")
    void testCountByFollower() {

        Follow follow1 = Follow.builder()
                .follower(user1)
                .following(user2)
                .build();
        Follow follow2 = Follow.builder()
                .follower(user1)
                .following(user3)
                .build();
        followRepository.save(follow1);
        followRepository.save(follow2);
        entityManager.flush();

        long followingCount = followRepository.countByFollower(user1);

        assertThat(followingCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Should delete follow relationship")
    void testDeleteFollow() {

        Follow follow = Follow.builder()
                .follower(user1)
                .following(user2)
                .build();
        Follow savedFollow = followRepository.save(follow);
        entityManager.flush();

        followRepository.delete(savedFollow);
        entityManager.flush();

        Optional<Follow> deletedFollow = followRepository.findById(savedFollow.getId());
        assertThat(deletedFollow).isEmpty();
    }

    @Nested
    @DisplayName("When testing follow relationships")
    class FollowRelationshipTests {

        @Test
        @DisplayName("Should allow user to follow multiple users")
        void testUserCanFollowMultipleUsers() {
            Follow follow1 = Follow.builder()
                    .follower(user1)
                    .following(user2)
                    .build();
            Follow follow2 = Follow.builder()
                    .follower(user1)
                    .following(user3)
                    .build();

            followRepository.save(follow1);
            followRepository.save(follow2);
            entityManager.flush();

            assertThat(followRepository.existsByFollowerAndFollowing(user1, user2)).isTrue();
            assertThat(followRepository.existsByFollowerAndFollowing(user1, user3)).isTrue();
        }

        @Test
        @DisplayName("Should allow user to be followed by multiple users")
        void testUserCanBeFollowedByMultipleUsers() {

            Follow follow1 = Follow.builder()
                    .follower(user1)
                    .following(user3)
                    .build();
            Follow follow2 = Follow.builder()
                    .follower(user2)
                    .following(user3)
                    .build();

            followRepository.save(follow1);
            followRepository.save(follow2);

            assertThat(followRepository.existsByFollowerAndFollowing(user1, user3)).isTrue();
            assertThat(followRepository.existsByFollowerAndFollowing(user2, user3)).isTrue();
        }

        @Test
        @DisplayName("Should handle bidirectional follow relationships")
        void testBidirectionalFollow() {
            Follow follow1 = Follow.builder()
                    .follower(user1)
                    .following(user2)
                    .build();
            Follow follow2 = Follow.builder()
                    .follower(user2)
                    .following(user1)
                    .build();

            followRepository.save(follow1);
            followRepository.save(follow2);
            entityManager.flush();

            assertThat(followRepository.existsByFollowerAndFollowing(user1, user2)).isTrue();
            assertThat(followRepository.existsByFollowerAndFollowing(user2, user1)).isTrue();
        }

        @Nested
        @DisplayName("When testing get followers functionality")
        class GetFollowerTests {

            @Test
            @DisplayName("Should get all followers of a user")
            void testGetAllUserFollower() {
                Follow follow1 = Follow.builder()
                        .follower(user2)
                        .following(user1)
                        .build();

                Follow follow2 = Follow.builder()
                        .follower(user3)
                        .following(user1)
                        .build();

                followRepository.save(follow1);
                followRepository.save(follow2);
                entityManager.flush();
                entityManager.clear();


                Pageable page = PageRequest.of(0, 12);
                Page<GetFollowResponse> followerPage = followRepository.getFollower(user1.getId(), page);
                Set<GetFollowResponse> followers = followerPage.toSet();

                assertThat(followers).hasSize(2);
                assertThat(followers)
                        .extracting(GetFollowResponse::getUsername)
                        .containsExactlyInAnyOrder("testuser2", "testuser3");
            }

            @Test
            @DisplayName("Should return empty page when user has no followers")
            void testGetFollowerWhenNoFollowers() {

                Pageable page = PageRequest.of(0, 12);
                Page<GetFollowResponse> followerPage = followRepository.getFollower(user1.getId(), page);

                assertThat(followerPage.getContent()).isEmpty();
                assertThat(followerPage.getTotalElements()).isZero();
            }

            @Test
            @DisplayName("Should respect pagination when getting followers")
            void testGetFollowerWithPagination() {
                Follow follow1 = Follow.builder()
                        .follower(user2)
                        .following(user1)
                        .build();

                Follow follow2 = Follow.builder()
                        .follower(user3)
                        .following(user1)
                        .build();

                Users user4 = Users.builder()
                        .id(UUID.randomUUID())
                        .username("testuser4")
                        .displayName("Test User 4")
                        .bio("Test bio 4")
                        .location("Test Location")
                        .dateOfBirth(LocalDate.of(2000, 1, 1))
                        .enable(true)
                        .isVerified(false)
                        .createdAt(LocalDateTime.now())
                        .build();
                entityManager.persist(user4);

                Follow follow3 = Follow.builder()
                        .follower(user4)
                        .following(user1)
                        .build();

                followRepository.save(follow1);
                followRepository.save(follow2);
                followRepository.save(follow3);
                entityManager.flush();
                entityManager.clear();

                Pageable page = PageRequest.of(0, 2);
                Page<GetFollowResponse> followerPage = followRepository.getFollower(user1.getId(), page);

                assertThat(followerPage.getContent()).hasSize(2);
                assertThat(followerPage.getTotalElements()).isEqualTo(3);
                assertThat(followerPage.getTotalPages()).isEqualTo(2);
            }
        }

        @Nested
        @DisplayName("When testing get following functionality")
        class GetFollowingTests {

            @Test
            @DisplayName("Should get all users that a user is following")
            void testGetAllUserFollowing() {
                Follow follow1 = Follow.builder()
                        .follower(user1)
                        .following(user2)
                        .build();

                Follow follow2 = Follow.builder()
                        .follower(user1)
                        .following(user3)
                        .build();

                followRepository.save(follow1);
                followRepository.save(follow2);
                entityManager.flush();
                entityManager.clear();

                Pageable page = PageRequest.of(0, 12);
                Page<GetFollowResponse> followingPage = followRepository.getFollowing(user1.getId(), page);
                Set<GetFollowResponse> following = followingPage.toSet();

                assertThat(following).hasSize(2);
                assertThat(following)
                        .extracting(GetFollowResponse::getUsername)
                        .containsExactlyInAnyOrder("testuser2", "testuser3");
            }

            @Test
            @DisplayName("Should return empty page when user is not following anyone")
            void testGetFollowingWhenNotFollowingAnyone() {

                Pageable page = PageRequest.of(0, 12);
                Page<GetFollowResponse> followingPage = followRepository.getFollowing(user1.getId(), page);

                assertThat(followingPage.getContent()).isEmpty();
                assertThat(followingPage.getTotalElements()).isZero();
            }
        }
    }
}
