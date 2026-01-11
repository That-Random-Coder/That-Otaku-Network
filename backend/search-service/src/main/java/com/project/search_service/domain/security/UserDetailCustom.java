package com.project.search_service.domain.security;

import com.project.search_service.domain.enums.Roles;
import com.project.search_service.service.RolePermissionMapping;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class UserDetailCustom implements UserDetails {

    private final UUID id;
    private Roles role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authoritySet = new HashSet<>(
                RolePermissionMapping.getAuthoritiesForRole(this.role));
        authoritySet.add(new SimpleGrantedAuthority("ROLE_" + this.role.toString()));
        return authoritySet;
    }

    @Override
    public String getPassword() {
        return "x";
    }

    @Override
    public String getUsername() {
        return "x";
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
        return true;
    }
}
