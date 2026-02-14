package com.codeops.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserId_returnsUserId() {
        UUID userId = UUID.randomUUID();
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals(userId, SecurityUtils.getCurrentUserId());
    }

    @Test
    void getCurrentUserId_noAuth_throws() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, SecurityUtils::getCurrentUserId);
    }

    @Test
    void getCurrentUserId_nullPrincipal_throws() {
        var auth = new UsernamePasswordAuthenticationToken(null, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThrows(AccessDeniedException.class, SecurityUtils::getCurrentUserId);
    }

    @Test
    void getCurrentUserId_wrongPrincipalType_throws() {
        var auth = new UsernamePasswordAuthenticationToken("not-a-uuid", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThrows(AccessDeniedException.class, SecurityUtils::getCurrentUserId);
    }

    @Test
    void hasRole_returnsTrue() {
        var auth = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(SecurityUtils.hasRole("ADMIN"));
        assertFalse(SecurityUtils.hasRole("OWNER"));
    }

    @Test
    void hasRole_noAuth_returnsFalse() {
        SecurityContextHolder.clearContext();
        assertFalse(SecurityUtils.hasRole("ADMIN"));
    }

    @Test
    void isAdmin_withAdminRole() {
        var auth = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertTrue(SecurityUtils.isAdmin());
    }

    @Test
    void isAdmin_withOwnerRole() {
        var auth = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null,
                List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertTrue(SecurityUtils.isAdmin());
    }

    @Test
    void isAdmin_withMemberRole_returnsFalse() {
        var auth = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertFalse(SecurityUtils.isAdmin());
    }
}
