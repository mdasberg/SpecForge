package com.specforge.platform.security;

import com.specforge.platform.identity.IdentityMirror;
import com.specforge.platform.identity.TokenIdentity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Mirrors the token's identity on every authenticated request, not only on the one that asks for
 * it, so any endpoint can rely on the {@code app_user} row existing and being current.
 */
@RequiredArgsConstructor
@Component
class IdentityMirrorFilter extends OncePerRequestFilter {

    private final IdentityMirror mirror;

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain chain)
            throws ServletException, IOException {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token && token.isAuthenticated()) {
            final Jwt jwt = token.getToken();
            mirror.mirror(TokenIdentity.of(jwt));
        }
        chain.doFilter(request, response);
    }
}
