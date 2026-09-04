package com.specforge.platform;

/**
 * Who is making the current request, as the rest of the product records them.
 *
 * <p>The subject id is Keycloak's, and it is the stable half: a display name is whatever the realm
 * says today, so anything that has to survive a rename stores the id and shows the name.
 */
public record Caller(String subjectId, String displayName) {}
