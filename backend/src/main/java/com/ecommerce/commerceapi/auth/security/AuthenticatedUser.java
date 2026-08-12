package com.ecommerce.commerceapi.auth.security;

import com.ecommerce.commerceapi.auth.domain.UserAccount;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AuthenticatedUser(
        Long id,
        String name,
        String email,
        String roleCode,
        String roleName,
        boolean active) implements UserDetails, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static AuthenticatedUser from(UserAccount userAccount) {
        return new AuthenticatedUser(
                userAccount.getId(),
                userAccount.getName(),
                userAccount.getEmail(),
                userAccount.getRole().getCode(),
                userAccount.getRole().getName(),
                userAccount.isActive());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleCode));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
