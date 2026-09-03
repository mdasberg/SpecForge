package com.specforge.platform.identity;

import com.specforge.platform.api.dto.ActorKind;
import com.specforge.platform.api.dto.Identity;
import com.specforge.platform.api.dto.Role;
import com.specforge.platform.api.generated.IdentityApi;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

/**
 * The mirrored identity of the caller. This is the only identity endpoint SpecForge has: there is
 * deliberately nothing here to change a password, a profile or a second factor, because those live
 * in Keycloak.
 *
 * <p>The route, the status codes and the response shape come from {@link IdentityApi}, which is
 * generated from {@code openapi/specforge-api.yaml}. Changing what this endpoint returns starts
 * with the contract, not with this class.
 */
@RequiredArgsConstructor
@RestController
class MeController implements IdentityApi {

    private final IdentityMirror mirror;

    /**
     * The caller is not an operation parameter — it is the security scheme — so the contract
     * declares none and the token is read from the security context rather than from a signature.
     */
    @Override
    public Identity getMe() {
        final Jwt jwt = ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken();
        final User user = mirror.mirror(TokenIdentity.of(jwt));
        return new Identity(user.subjectId(), user.displayName(), actorKind(user), roles(user))
                .avatarUrl(user.avatarUrl());
    }

    /**
     * The domain enums and the contract enums are deliberately separate types: the wire format is
     * allowed to outlive a rename inside the application, and the compiler catches the day they
     * stop lining up.
     */
    private static ActorKind actorKind(final User user) {
        return ActorKind.fromValue(user.actorKind().name());
    }

    private static List<Role> roles(final User user) {
        return user.roles().stream().sorted().map(role -> Role.fromValue(role.name())).toList();
    }
}
