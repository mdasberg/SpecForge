package com.specforge.discussion.service;

import com.specforge.review.AnchorState;

/**
 * What a head advance does to one thread's own anchor state, given what it did to the thread's
 * section key.
 *
 * <p>The transition is one-way: once a thread has drifted from the text it quoted — stale because
 * the section changed, orphaned because the section is gone — a later head that happens to leave
 * the section untouched does not undo that. Each {@link com.specforge.review.ReviewHeadAdvanced}
 * event only classifies a section against the head immediately before it, so "unchanged this hop"
 * is not the same claim as "matches what this thread originally quoted".
 */
final class ThreadAnchorCarry {

    private ThreadAnchorCarry() {}

    static AnchorState next(final AnchorState current, final AnchorState computedThisHop) {
        if (computedThisHop == AnchorState.ORPHANED) {
            return AnchorState.ORPHANED;
        }
        return current == AnchorState.STALE ? AnchorState.STALE : computedThisHop;
    }
}
