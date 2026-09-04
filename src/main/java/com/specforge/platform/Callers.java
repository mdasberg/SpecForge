package com.specforge.platform;

/**
 * The authenticated caller, for a capability that has to attribute what it is about to write.
 *
 * <p>It exists so that reading a token stays in one place. Every module that attributes an action
 * would otherwise pull claims out of the JWT itself, and the day the realm changes which claim
 * carries a display name, they would disagree about who did what.
 */
public interface Callers {

    /** Throws if there is no authenticated caller, which on an authenticated route cannot happen. */
    Caller current();
}
