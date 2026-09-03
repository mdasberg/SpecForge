package com.specforge.platform.identity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * The local mirror of a Keycloak identity, keyed by the token's subject id so a rename in the
 * provider never orphans a comment or a verdict. SpecForge owns no credential: everything here is
 * refreshed from the token on each authenticated request.
 *
 * <p>The table is {@code app_user} rather than {@code user} because {@code user} is a reserved
 * word in PostgreSQL and would have to be quoted at every use site.
 */
@Entity
@Table(name = "app_user")
public class User {

    @Id
    @Column(name = "subject_id", nullable = false, length = 64)
    private String subjectId;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_kind", nullable = false, length = 16)
    private ActorKind actorKind;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "app_user_role", joinColumns = @JoinColumn(name = "user_subject_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
        // for JPA
    }

    User(final String subjectId, final String displayName, final String avatarUrl, final ActorKind actorKind,
            final Set<Role> roles, final Instant now) {
        this.subjectId = subjectId;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.actorKind = actorKind;
        this.roles = EnumSet.copyOf(roles);
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Applies what the current token says. Returns whether anything actually changed, so a request
     * that presents the same claims as the last one does not write.
     */
    boolean refreshFrom(final String displayName, final String avatarUrl, final ActorKind actorKind,
            final Set<Role> roles, final Instant now) {
        if (this.displayName.equals(displayName)
                && java.util.Objects.equals(this.avatarUrl, avatarUrl)
                && this.actorKind == actorKind
                && this.roles.equals(roles)) {
            return false;
        }
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.actorKind = actorKind;
        this.roles = EnumSet.copyOf(roles);
        this.updatedAt = now;
        return true;
    }

    public String subjectId() {
        return subjectId;
    }

    public String displayName() {
        return displayName;
    }

    public String avatarUrl() {
        return avatarUrl;
    }

    public ActorKind actorKind() {
        return actorKind;
    }

    public Set<Role> roles() {
        return EnumSet.copyOf(roles);
    }
}
