package com.project.content_service.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Permission {

    USER_GET("user:get"),
    USER_WRITE("user:write"),
    USER_SEARCH("user:search"),
    USER_DISABLE("user:disable"),
    ADMIN_DISABLE("admin:disable"),
    CONTENT_VIEW("content:view"),
    CONTENT_POST("content:post"),
    CONTENT_EDIT("content:edit"),
    CONTENT_DELETE("content:delete"),
    CONTENT_SEARCH("content:search"),
    CONTENT_LIKE("content:like"),
    CONTENT_COMMENT("content:comment"),
    CONTENT_DISLIKE("content:dislike"),
    USER_FOLLOW("user:follow"),
    USER_UNFOLLOW("user:unfollow"),
    ROLE_ASSIGN("role:assign");

    private final String permission;

}
