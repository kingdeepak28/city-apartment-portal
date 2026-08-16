package com.societyportal.backend.security;

import com.societyportal.backend.domain.enums.AccountType;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Stateless security principal built directly from validated JWT claims -
 * represents either an {@code AdminUser} or a member {@code User}.
 */
@Getter
public class AuthPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String name;
    private final AccountType accountType;
    private final String role; // SUPER_ADMIN / ADMIN / UPLOADER / MEMBER

    public AuthPrincipal(UUID id, String email, String name, AccountType accountType, String role) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.accountType = accountType;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public boolean isAdmin() {
        return accountType == AccountType.ADMIN;
    }
}
