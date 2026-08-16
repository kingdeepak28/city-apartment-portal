package com.societyportal.backend.security;

import org.springframework.security.core.context.SecurityContextHolder;

/** Convenience accessor for the authenticated principal within a request. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthPrincipal get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return principal;
    }
}
