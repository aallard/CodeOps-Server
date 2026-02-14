package com.codeops.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Servlet filter that authenticates incoming HTTP requests by extracting and validating
 * JWT tokens from the {@code Authorization} header.
 *
 * <p>When a valid Bearer token is present, this filter extracts the user ID, email, and
 * roles from the token claims and populates the Spring {@link SecurityContextHolder} with a
 * {@link UsernamePasswordAuthenticationToken}. The principal is set to the user's {@link UUID},
 * and roles are mapped to {@link SimpleGrantedAuthority} instances with a {@code ROLE_} prefix.</p>
 *
 * <p>Requests without an {@code Authorization} header or with an invalid token are allowed
 * to proceed through the filter chain unauthenticated, leaving authorization decisions to
 * downstream security configuration.</p>
 *
 * @see JwtTokenProvider
 * @see SecurityConfig
 * @see SecurityUtils
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Extracts the JWT token from the {@code Authorization} header, validates it, and sets
     * the Spring Security authentication context if valid.
     *
     * <p>If the header is missing or does not start with {@code "Bearer "}, the request is
     * passed through without authentication. If the token is present but invalid, a warning
     * is logged with the client's remote IP address, and the request proceeds unauthenticated.</p>
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain to pass the request/response to
     * @throws ServletException if a servlet error occurs during filtering
     * @throws IOException      if an I/O error occurs during filtering
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (jwtTokenProvider.validateToken(token)) {
            UUID userId = jwtTokenProvider.getUserIdFromToken(token);
            String email = jwtTokenProvider.getEmailFromToken(token);
            List<String> roles = jwtTokenProvider.getRolesFromToken(token);

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            log.warn("Invalid JWT token from IP: {}", request.getRemoteAddr());
        }

        filterChain.doFilter(request, response);
    }
}
