package com.specforge.agent.entity;

/**
 * What a human did about a finding. {@link #UNDONE} is a row of its own rather than a deletion:
 * dismissing something and then changing your mind is what happened, and a history that hides the
 * first half is not a history.
 */
public enum DispositionKind {
    ACCEPTED,
    DISMISSED,
    UNDONE
}
