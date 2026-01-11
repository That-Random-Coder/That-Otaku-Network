package com.project.recommendation_service.service;


import com.project.recommendation_service.domain.enums.Permission;
import com.project.recommendation_service.domain.enums.Roles;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RolePermissionMapping {
    private static final Set<Permission> USER_PERMISSIONS =
            new HashSet<>(Set.of(
                    Permission.USER_GET,
                    Permission.USER_SEARCH,
                    Permission.USER_FOLLOW,
                    Permission.USER_UNFOLLOW,
                    Permission.CONTENT_LIKE,
                    Permission.CONTENT_POST,
                    Permission.CONTENT_DISLIKE,
                    Permission.CONTENT_COMMENT,
                    Permission.CONTENT_VIEW,
                    Permission.CONTENT_SEARCH
            ));

    private static final Set<Permission> ADMIN_PERMISSIONS =
            new HashSet<>(USER_PERMISSIONS);

    static {
        ADMIN_PERMISSIONS.addAll(Set.of(
                Permission.USER_DISABLE,
                Permission.CONTENT_DELETE
        ));
    }

    private static final Set<Permission> MODERATOR_PERMISSION =
            new HashSet<>(ADMIN_PERMISSIONS);

    static {
        MODERATOR_PERMISSION.addAll(Set.of(
                Permission.ADMIN_DISABLE , Permission.CONTENT_EDIT , Permission.USER_WRITE , Permission.ROLE_ASSIGN
        ));
    }

    private static final Map<Roles, Set<Permission>> ROLE_PERMISSIONS =
            Map.of(
                    Roles.USER, Set.copyOf(USER_PERMISSIONS),
                    Roles.ADMIN, Set.copyOf(ADMIN_PERMISSIONS),
                    Roles.MODERATOR , Set.copyOf(MODERATOR_PERMISSION)
            );

    public static Set<SimpleGrantedAuthority> getAuthoritiesForRole(Roles role) {
        return ROLE_PERMISSIONS.get(role).stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
                .collect(Collectors.toSet());
    }


}

