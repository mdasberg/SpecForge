package com.specforge.platform.identity;


/**
 * The only roles SpecForge recognises. They come from the token's realm roles and from nowhere
 * else, so a role change in Keycloak takes effect on the next token.
 */
public enum Role {
    REVIEWER,
    ARCHITECT,
    ADMIN;

    /** Every authenticated identity is at least a reviewer. */
    public static final Role DEFAULT = REVIEWER;

    /** The Spring Security authority name for this role. */
    public String authority() {
        return "ROLE_" + name();
    }
}
