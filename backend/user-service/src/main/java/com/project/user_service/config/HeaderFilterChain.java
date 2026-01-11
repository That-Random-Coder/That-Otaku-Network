package com.project.user_service.config;

import com.project.user_service.domain.enums.Roles;
import com.project.user_service.domain.security.UserDetailCustom;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HeaderFilterChain extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String id = request.getHeader("User-Id");
        String role = request.getHeader("User-Role");

        if (id == null || id.isBlank() || role == null || role.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UUID uuid = UUID.fromString(id);
            Roles roleEnum = Roles.valueOf(role);

            UserDetailCustom userDetail = new UserDetailCustom(uuid, roleEnum);

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetail,
                    null, userDetail.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid User-Id or User-Role header: id='{}', role='{}'. Continuing unauthenticated.", id, role);
        }
        filterChain.doFilter(request, response);
    }
}
