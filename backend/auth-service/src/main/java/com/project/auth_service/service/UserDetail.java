package com.project.auth_service.service;

import com.project.auth_service.domain.entity.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@Builder
public class UserDetail implements UserDetails {

    private final UserProfile user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authoritySet = new HashSet<>();
        authoritySet.addAll(RolePermissionMapping.getAuthoritiesForRole(user.getRoles()));
        authoritySet.add(new SimpleGrantedAuthority("ROLE_"+user.getRoles().toString()));
        return authoritySet;
    }

    public UserProfile getUser() {
        return user;
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
