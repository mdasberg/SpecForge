package com.specforge.catalog.internal;

import static com.specforge.catalog.SpecStatus.APPROVED;
import static com.specforge.catalog.SpecStatus.CHANGES_REQUESTED;
import static com.specforge.catalog.SpecStatus.DRAFT;
import static com.specforge.catalog.SpecStatus.IMPLEMENTED;
import static com.specforge.catalog.SpecStatus.IN_REVIEW;

import com.specforge.catalog.SpecStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The only place a specification's status is judged. Reviews, approvals and imports all route
 * their transitions through here, so "which moves are legal" is one table rather than a condition
 * repeated in every caller that happens to set the field.
 */
final class SpecLifecycle {

    private static final Map<SpecStatus, Set<SpecStatus>> LEGAL = new EnumMap<>(SpecStatus.class);

    static {
        LEGAL.put(DRAFT, EnumSet.of(IN_REVIEW));
        LEGAL.put(CHANGES_REQUESTED, EnumSet.of(IN_REVIEW));
        LEGAL.put(IN_REVIEW, EnumSet.of(CHANGES_REQUESTED, APPROVED));
        // An approved or implemented specification re-enters review when a new change is proposed;
        // the approval it already earned stays in history rather than being erased.
        LEGAL.put(APPROVED, EnumSet.of(IMPLEMENTED, IN_REVIEW));
        LEGAL.put(IMPLEMENTED, EnumSet.of(IN_REVIEW));
    }

    private SpecLifecycle() {}

    static boolean isLegal(SpecStatus from, SpecStatus to) {
        return LEGAL.getOrDefault(from, Set.of()).contains(to);
    }

    /** Applies the transition, or refuses it. A move to the status already held is a no-op. */
    static void transition(SpecDocument document, SpecStatus target, java.time.Instant now) {
        if (document.status() == target) {
            return;
        }
        if (!isLegal(document.status(), target)) {
            throw new IllegalSpecTransitionException(document.id(), document.status(), target);
        }
        document.changeStatus(target, now);
    }
}
