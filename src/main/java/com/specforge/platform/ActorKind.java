package com.specforge.platform;

/**
 * Whether an identity belongs to a person or to an automated check runner. This is an API field
 * rather than a UI convention: every comment, finding and audit entry carries it, and the approval
 * rules refuse a verdict from an {@link #AGENT}.
 *
 * <p>It lives in the platform's published package, not inside {@code identity}, because every
 * capability module that attributes an action — discussions, approval, automated review — needs the
 * type itself, not just a value mirrored through {@link Caller}.
 */
public enum ActorKind {
    HUMAN,
    AGENT
}
