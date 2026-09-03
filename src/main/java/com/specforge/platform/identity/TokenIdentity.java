package com.specforge.platform.identity;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.jwt.Jwt;


/**
 * What a Keycloak access token says about who is calling. Reading the claims lives here so the
 * role converter and the identity mirror cannot disagree about them.
 */
public record TokenIdentity(String subjectId, String displayName, String avatarUrl, ActorKind actorKind, Set<Role> roles) {

    /**
     * Keycloak names a client's service-account user {@code service-account-<clientId>}. That
     * prefix is the only signal in the token that the caller is a check runner rather than a
     * person, so it is what decides {@link ActorKind}.
     */
    private static final String SERVICE_ACCOUNT_PREFIX = "service-account-";

    public static TokenIdentity of(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        String name = jwt.getClaimAsString("name");
        String displayName = firstNonBlank(name, username, jwt.getSubject());
        ActorKind actorKind = username != null && username.startsWith(SERVICE_ACCOUNT_PREFIX)
                ? ActorKind.AGENT
                : ActorKind.HUMAN;
        return new TokenIdentity(jwt.getSubject(), displayName, jwt.getClaimAsString("picture"), actorKind, realmRoles(jwt));
    }

    /**
     * Realm roles live under {@code realm_access.roles}. Anything there that is not one of
     * SpecForge's three roles is ignored rather than rejected — a realm carries roles for other
     * clients too — and an identity with none of them is still a reviewer.
     */
    private static Set<Role> realmRoles(Jwt jwt) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> claimed) {
            for (Object role : claimed) {
                if (role instanceof String name) {
                    for (Role known : Role.values()) {
                        if (known.name().equals(name)) {
                            roles.add(known);
                        }
                    }
                }
            }
        }
        if (roles.isEmpty()) {
            roles.add(Role.DEFAULT);
        }
        return roles;
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "unknown";
    }
}
