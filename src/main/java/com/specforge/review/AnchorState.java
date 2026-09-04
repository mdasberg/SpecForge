package com.specforge.review;

/**
 * What became of an anchor when the review's head advanced.
 *
 * <p>Anchors are carried, never fuzzily reattached. A comment written against version 2 either still
 * addresses the same section in version 4, addresses a section whose text has since been rewritten,
 * or addresses a section that is gone — and it says which. The alternative, guessing at the nearest
 * matching text, silently moves a reviewer's objection onto a paragraph they never read.
 */
public enum AnchorState {

    /** The section kept its key and its text: the anchor means what it meant. */
    CURRENT,

    /** The section is still there under the same key, but its text was rewritten. */
    STALE,

    /** The section's key is gone from the head. The anchor renders against its quoted original. */
    ORPHANED
}
