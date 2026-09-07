package com.specforge.platform;

/**
 * Enough of a mirrored identity to name someone and link to them, for a capability that has to
 * resolve an {@code @}-mention or list who a specification's reviewers could be — without reaching
 * into the identity module's own tables to get it.
 */
public record MemberRef(String subjectId, String displayName, String handle, String avatarUrl, ActorKind actorKind) {}
