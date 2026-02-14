package com.codeops.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utility class providing static helper methods for accessing the current Spring Security
 * authentication context.
 *
 * <p>Methods in this class read from {@link SecurityContextHolder} and expect the principal
 * to be a {@link UUID} (as set by {@link JwtAuthFilter}) and authorities to follow the
 * {@code ROLE_} prefix convention.</p>
 *
 * @see JwtAuthFilter
 * @see SecurityConfig
 */
public final class SecurityUtils {
    private SecurityUtils() {}

    /**
     * Retrieves the UUID of the currently authenticated user from the Spring Security context.
     *
     * @return the authenticated user's UUID
     * @throws org.springframework.security.access.AccessDeniedException if no authentication
     *         is present or the principal is not a {@link UUID}
     */
    public static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new org.springframework.security.access.AccessDeniedException("No authenticated user");
        }
        if (!(auth.getPrincipal() instanceof UUID userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid authentication principal");
        }
        return userId;
    }

    /**
     * Checks whether the currently authenticated user has the specified role.
     *
     * <p>The role name is matched with a {@code ROLE_} prefix against the granted authorities
     * in the security context (e.g., passing {@code "ADMIN"} checks for {@code ROLE_ADMIN}).</p>
     *
     * @param role the role name to check (without the {@code ROLE_} prefix)
     * @return {@code true} if the current user has the specified role, {@code false} if they
     *         do not or if no authentication is present
     */
    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    /**
     * Checks whether the currently authenticated user has administrative privileges,
     * defined as having either the {@code ADMIN} or {@code OWNER} role.
     *
     * @return {@code true} if the current user has the ADMIN or OWNER role, {@code false} otherwise
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN") || hasRole("OWNER");
    }
}
