package com.specforge.platform.identity;

/**
 * Whether an identity belongs to a person or to an automated check runner. This is an API field
 * rather than a UI convention: every comment, finding and audit entry carries it, and the approval
 * rules refuse a verdict from an {@link #AGENT}.
 */
public enum ActorKind {
    HUMAN,
    AGENT
}
