package com.project.user_service.service;

import com.project.user_service.domain.enums.Permission;
import com.project.user_service.domain.enums.Roles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RolePermissionMappingTest {

        @Nested
        @DisplayName("When getting authorities for USER role")
        class UserRoleTests {

                @Test
                @DisplayName("Should contain all basic user permissions")
                void testUserRolePermissions() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.USER);
                        Set<String> permissions = authorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(permissions).containsExactlyInAnyOrder(
                                        "user:get",
                                        "user:search",
                                        "user:follow",
                                        "user:unfollow",
                                        "content:like",
                                        "content:post",
                                        "content:dislike",
                                        "content:comment",
                                        "content:view",
                                        "content:search");
                }

                @Test
                @DisplayName("Should not contain admin permissions")
                void testUserRoleDoesNotHaveAdminPermissions() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.USER);
                        Set<String> permissions = authorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(permissions).doesNotContain(
                                        "user:disable",
                                        "content:delete",
                                        "admin:disable",
                                        "content:edit",
                                        "user:write",
                                        "role:assign");
                }

                @Test
                @DisplayName("Should have exactly 10 permissions")
                void testUserRolePermissionCount() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.USER);

                        assertThat(authorities).hasSize(10);
                }

                @Test
                @DisplayName("Should contain USER_GET permission")
                void testUserRoleHasUserGetPermission() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.USER);
                        Set<String> permissions = authorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(permissions).contains(Permission.USER_GET.getPermission());
                }

                @Test
                @DisplayName("Should contain CONTENT_POST permission")
                void testUserRoleHasContentPostPermission() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.USER);
                        Set<String> permissions = authorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(permissions).contains(Permission.CONTENT_POST.getPermission());
                }
        }

        @Nested
        @DisplayName("When getting authorities for ADMIN role")
        class AdminRoleTests {

                @Test
                @DisplayName("Should contain all user permissions plus admin-specific permissions")
                void testAdminRolePermissions() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.ADMIN);
                        Set<String> permissions = authorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(permissions).contains(
                                        "user:get",
                                        "user:search",
                                        "user:follow",
                                        "user:unfollow",
                                        "content:like",
                                        "content:post",
                                        "content:dislike",
                                        "content:comment",
                                        "content:view",
                                        "content:search");

                        assertThat(permissions).contains(
                                        "user:disable",
                                        "content:delete");
                }

                @Test
                @DisplayName("Should have exactly 12 permissions")
                void testAdminRolePermissionCount() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.ADMIN);

                        assertThat(authorities).hasSize(12);
                }

                @Test
                @DisplayName("Should not contain moderator-only permissions")
                void testAdminRoleDoesNotHaveModeratorPermissions() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.ADMIN);
                        Set<String> permissions = authorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(permissions).doesNotContain(
                                        "admin:disable",
                                        "content:edit",
                                        "user:write",
                                        "role:assign");
                }

                @Test
                @DisplayName("Should have USER_DISABLE permission")
                void testAdminRoleHasUserDisablePermission() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.ADMIN);
                        Set<String> permissions = authorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(permissions).contains(Permission.USER_DISABLE.getPermission());
                }

                @Test
                @DisplayName("Should have CONTENT_DELETE permission")
                void testAdminRoleHasContentDeletePermission() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.ADMIN);
                        Set<String> permissions = authorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(permissions).contains(Permission.CONTENT_DELETE.getPermission());
                }
        }

        @Nested
        @DisplayName("When getting authorities for MODERATOR role")
        class ModeratorRoleTests {

                @Test
                @DisplayName("Should contain all admin permissions plus moderator-specific permissions")
                void testModeratorRolePermissions() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.MODERATOR);
                        Set<String> permissions = authorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(permissions).contains(
                                        "user:get",
                                        "user:search",
                                        "user:follow",
                                        "user:unfollow",
                                        "content:like",
                                        "content:post",
                                        "content:dislike",
                                        "content:comment",
                                        "content:view",
                                        "content:search",
                                        "user:disable",
                                        "content:delete");

                        assertThat(permissions).contains(
                                        "admin:disable",
                                        "content:edit",
                                        "user:write",
                                        "role:assign");
                }

                @Test
                @DisplayName("Should have exactly 16 permissions")
                void testModeratorRolePermissionCount() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.MODERATOR);

                        assertThat(authorities).hasSize(16);
                }

                @Test
                @DisplayName("Should have ADMIN_DISABLE permission")
                void testModeratorRoleHasAdminDisablePermission() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.MODERATOR);
                        Set<String> permissions = authorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(permissions).contains(Permission.ADMIN_DISABLE.getPermission());
                }

                @Test
                @DisplayName("Should have CONTENT_EDIT permission")
                void testModeratorRoleHasContentEditPermission() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.MODERATOR);
                        Set<String> permissions = authorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(permissions).contains(Permission.CONTENT_EDIT.getPermission());
                }

                @Test
                @DisplayName("Should have USER_WRITE permission")
                void testModeratorRoleHasUserWritePermission() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.MODERATOR);
                        Set<String> permissions = authorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(permissions).contains(Permission.USER_WRITE.getPermission());
                }

                @Test
                @DisplayName("Should have ROLE_ASSIGN permission")
                void testModeratorRoleHasRoleAssignPermission() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.MODERATOR);
                        Set<String> permissions = authorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(permissions).contains(Permission.ROLE_ASSIGN.getPermission());
                }
        }

        @Nested
        @DisplayName("When comparing role hierarchies")
        class RoleHierarchyTests {

                @Test
                @DisplayName("Should verify ADMIN has more permissions than USER")
                void testAdminHasMorePermissionsThanUser() {
                        Set<SimpleGrantedAuthority> userAuthorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.USER);
                        Set<SimpleGrantedAuthority> adminAuthorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.ADMIN);

                        assertThat(adminAuthorities.size()).isGreaterThan(userAuthorities.size());
                }

                @Test
                @DisplayName("Should verify MODERATOR has more permissions than ADMIN")
                void testModeratorHasMorePermissionsThanAdmin() {
                        Set<SimpleGrantedAuthority> adminAuthorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.ADMIN);
                        Set<SimpleGrantedAuthority> moderatorAuthorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.MODERATOR);

                        assertThat(moderatorAuthorities.size()).isGreaterThan(adminAuthorities.size());
                }

                @Test
                @DisplayName("Should verify ADMIN contains all USER permissions")
                void testAdminContainsAllUserPermissions() {
                        Set<SimpleGrantedAuthority> userAuthorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.USER);
                        Set<SimpleGrantedAuthority> adminAuthorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.ADMIN);

                        Set<String> userPermissions = userAuthorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());
                        Set<String> adminPermissions = adminAuthorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(adminPermissions).containsAll(userPermissions);
                }

                @Test
                @DisplayName("Should verify MODERATOR contains all ADMIN permissions")
                void testModeratorContainsAllAdminPermissions() {
                        Set<SimpleGrantedAuthority> adminAuthorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.ADMIN);
                        Set<SimpleGrantedAuthority> moderatorAuthorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.MODERATOR);

                        Set<String> adminPermissions = adminAuthorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());
                        Set<String> moderatorPermissions = moderatorAuthorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(moderatorPermissions).containsAll(adminPermissions);
                }

                @Test
                @DisplayName("Should verify MODERATOR contains all USER permissions")
                void testModeratorContainsAllUserPermissions() {
                        Set<SimpleGrantedAuthority> userAuthorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.USER);
                        Set<SimpleGrantedAuthority> moderatorAuthorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.MODERATOR);

                        Set<String> userPermissions = userAuthorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());
                        Set<String> moderatorPermissions = moderatorAuthorities.stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(moderatorPermissions).containsAll(userPermissions);
                }
        }

        @Nested
        @DisplayName("When testing authority types")
        class AuthorityTypeTests {

                @Test
                @DisplayName("Should return SimpleGrantedAuthority instances")
                void testReturnsSimpleGrantedAuthority() {
                        Set<SimpleGrantedAuthority> authorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.USER);

                        assertThat(authorities).allMatch(auth -> auth instanceof SimpleGrantedAuthority);
                }

                @Test
                @DisplayName("Should return non-null authorities for all roles")
                void testReturnsNonNullAuthorities() {
                        assertThat(RolePermissionMapping.getAuthoritiesForRole(Roles.USER)).isNotNull();
                        assertThat(RolePermissionMapping.getAuthoritiesForRole(Roles.ADMIN)).isNotNull();
                        assertThat(RolePermissionMapping.getAuthoritiesForRole(Roles.MODERATOR)).isNotNull();
                }

                @Test
                @DisplayName("Should return non-empty authorities for all roles")
                void testReturnsNonEmptyAuthorities() {
                        assertThat(RolePermissionMapping.getAuthoritiesForRole(Roles.USER)).isNotEmpty();
                        assertThat(RolePermissionMapping.getAuthoritiesForRole(Roles.ADMIN)).isNotEmpty();
                        assertThat(RolePermissionMapping.getAuthoritiesForRole(Roles.MODERATOR)).isNotEmpty();
                }

                @Test
                @DisplayName("Should return immutable set for each role")
                void testReturnsImmutableSets() {
                        Set<SimpleGrantedAuthority> userAuthorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.USER);
                        Set<SimpleGrantedAuthority> adminAuthorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.ADMIN);
                        Set<SimpleGrantedAuthority> moderatorAuthorities = RolePermissionMapping
                                        .getAuthoritiesForRole(Roles.MODERATOR);

                        assertThat(userAuthorities)
                                        .isNotSameAs(RolePermissionMapping.getAuthoritiesForRole(Roles.USER));
                        assertThat(adminAuthorities)
                                        .isNotSameAs(RolePermissionMapping.getAuthoritiesForRole(Roles.ADMIN));
                        assertThat(moderatorAuthorities)
                                        .isNotSameAs(RolePermissionMapping.getAuthoritiesForRole(Roles.MODERATOR));
                }
        }

        @Nested
        @DisplayName("When testing specific permission combinations")
        class PermissionCombinationTests {

                @Test
                @DisplayName("Should verify only ADMIN and MODERATOR can disable users")
                void testUserDisablePermission() {
                        Set<String> userPermissions = RolePermissionMapping.getAuthoritiesForRole(Roles.USER).stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());
                        Set<String> adminPermissions = RolePermissionMapping.getAuthoritiesForRole(Roles.ADMIN).stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());
                        Set<String> moderatorPermissions = RolePermissionMapping.getAuthoritiesForRole(Roles.MODERATOR)
                                        .stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(userPermissions).doesNotContain("user:disable");
                        assertThat(adminPermissions).contains("user:disable");
                        assertThat(moderatorPermissions).contains("user:disable");
                }

                @Test
                @DisplayName("Should verify only ADMIN and MODERATOR can delete content")
                void testContentDeletePermission() {
                        Set<String> userPermissions = RolePermissionMapping.getAuthoritiesForRole(Roles.USER).stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());
                        Set<String> adminPermissions = RolePermissionMapping.getAuthoritiesForRole(Roles.ADMIN).stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());
                        Set<String> moderatorPermissions = RolePermissionMapping.getAuthoritiesForRole(Roles.MODERATOR)
                                        .stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(userPermissions).doesNotContain("content:delete");
                        assertThat(adminPermissions).contains("content:delete");
                        assertThat(moderatorPermissions).contains("content:delete");
                }

                @Test
                @DisplayName("Should verify only MODERATOR can edit content")
                void testContentEditPermission() {
                        Set<String> userPermissions = RolePermissionMapping.getAuthoritiesForRole(Roles.USER).stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());
                        Set<String> adminPermissions = RolePermissionMapping.getAuthoritiesForRole(Roles.ADMIN).stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());
                        Set<String> moderatorPermissions = RolePermissionMapping.getAuthoritiesForRole(Roles.MODERATOR)
                                        .stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(userPermissions).doesNotContain("content:edit");
                        assertThat(adminPermissions).doesNotContain("content:edit");
                        assertThat(moderatorPermissions).contains("content:edit");
                }

                @Test
                @DisplayName("Should verify only MODERATOR can assign roles")
                void testRoleAssignPermission() {
                        Set<String> userPermissions = RolePermissionMapping.getAuthoritiesForRole(Roles.USER).stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());
                        Set<String> adminPermissions = RolePermissionMapping.getAuthoritiesForRole(Roles.ADMIN).stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());
                        Set<String> moderatorPermissions = RolePermissionMapping.getAuthoritiesForRole(Roles.MODERATOR)
                                        .stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(userPermissions).doesNotContain("role:assign");
                        assertThat(adminPermissions).doesNotContain("role:assign");
                        assertThat(moderatorPermissions).contains("role:assign");
                }

                @Test
                @DisplayName("Should verify all roles can view content")
                void testContentViewPermission() {
                        Set<String> userPermissions = RolePermissionMapping.getAuthoritiesForRole(Roles.USER).stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());
                        Set<String> adminPermissions = RolePermissionMapping.getAuthoritiesForRole(Roles.ADMIN).stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());
                        Set<String> moderatorPermissions = RolePermissionMapping.getAuthoritiesForRole(Roles.MODERATOR)
                                        .stream()
                                        .map(SimpleGrantedAuthority::getAuthority)
                                        .collect(Collectors.toSet());

                        assertThat(userPermissions).contains("content:view");
                        assertThat(adminPermissions).contains("content:view");
                        assertThat(moderatorPermissions).contains("content:view");
                }
        }
}

