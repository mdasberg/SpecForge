package com.specforge.platform;

/**
 * Who is making the current request, as the rest of the product records them.
 *
 * <p>The subject id is Keycloak's, and it is the stable half: a display name is whatever the realm
 * says today, so anything that has to survive a rename stores the id and shows the name. The actor
 * kind rides along so a capability that attributes a write — a comment, a verdict, a finding — never
 * has to look the caller back up in the identity mirror just to learn whether they are a person.
 */
public record Caller(String subjectId, String displayName, ActorKind actorKind) {}
