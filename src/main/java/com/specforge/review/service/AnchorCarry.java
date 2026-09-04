package com.specforge.review.service;

import com.specforge.catalog.SpecText;
import com.specforge.platform.api.dto.DiffSection;
import com.specforge.review.AnchorState;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a head update does to the anchors written against the head it replaces.
 *
 * <p>It is the section classification read a second way, which is the point: an anchor's fate and a
 * reviewer's diff are the same judgement about the same text, so they are computed once. A section
 * whose key and text survived carries its anchors unchanged; one whose text was rewritten keeps them
 * but flags them stale; one whose key is gone orphans them. Nothing is reattached by resemblance.
 */
final class AnchorCarry {

    private AnchorCarry() {}

    static Map<String, AnchorState> states(final SpecText previousHead, final SpecText newHead) {
        final Map<String, AnchorState> states = new LinkedHashMap<>();
        for (final DiffSection section : SpecDiffs.compute(previousHead, newHead)) {
            switch (section.getChange()) {
                case UNCHANGED -> states.put(section.getAnchorKey(), AnchorState.CURRENT);
                case MODIFIED -> states.put(section.getAnchorKey(), AnchorState.STALE);
                case REMOVED -> states.put(section.getAnchorKey(), AnchorState.ORPHANED);
                // A section the new head introduced held no anchors, so there is nothing to say
                // about it: leaving it out is what keeps "absent means undecided" true.
                case ADDED -> { }
            }
        }
        return Map.copyOf(states);
    }
}
