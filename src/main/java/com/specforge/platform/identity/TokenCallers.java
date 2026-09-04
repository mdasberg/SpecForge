package com.specforge.platform.identity;

import com.specforge.platform.Caller;
import com.specforge.platform.Callers;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Reads the caller off the validated Keycloak token, through the same claim rules as everything else. */
@Component
class TokenCallers implements Callers {

    @Override
    public Caller current() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw new IllegalStateException("No authenticated caller on an authenticated route.");
        }
        final TokenIdentity identity = TokenIdentity.of(token.getToken());
        return new Caller(identity.subjectId(), identity.displayName());
    }
}
