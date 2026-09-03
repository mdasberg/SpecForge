package com.specforge.platform.identity;

import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the local {@code app_user} row in step with the token. The first authenticated request for
 * a subject creates the row; every later one refreshes the display name, avatar and roles, so a
 * role removed in Keycloak disappears here as soon as the next token arrives.
 */
@RequiredArgsConstructor
@Service
public class IdentityMirror {

    private final UserRepository users;
    private final Clock clock;

    // ponytail: reads the row on every authenticated request and writes only when the claims
    // actually differ. If that read ever shows up in a profile, cache by subject id with the
    // token's expiry as the TTL.
    @Transactional
    public User mirror(final TokenIdentity identity) {
        final Instant now = clock.instant();
        return users.findById(identity.subjectId())
                .map(existing -> {
                    if (existing.refreshFrom(identity.displayName(), identity.avatarUrl(), identity.actorKind(), identity.roles(), now)) {
                        users.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> users.save(new User(
                        identity.subjectId(),
                        identity.displayName(),
                        identity.avatarUrl(),
                        identity.actorKind(),
                        identity.roles(),
                        now)));
    }
}
