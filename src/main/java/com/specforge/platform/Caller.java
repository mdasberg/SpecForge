package com.specforge.platform;

import java.util.Set;

/**
 * Who is making the current request, as the rest of the product records them.
 *
 * <p>The subject id is Keycloak's, and it is the stable half: a display name is whatever the realm
 * says today, so anything that has to survive a rename stores the id and shows the name. The actor
 * kind rides along so a capability that attributes a write — a comment, a verdict, a finding — never
 * has to look the caller back up in the identity mirror just to learn whether they are a person.
 *
 * <p>{@code roles} carries the realm role names (e.g. {@code "ARCHITECT"}) rather than
 * {@code platform.identity.Role} itself: that type is internal to the identity module, and the
 * approval capability only ever needs to test membership, not the enum.
 */
public record Caller(String subjectId, String displayName, ActorKind actorKind, Set<String> roles) {}
