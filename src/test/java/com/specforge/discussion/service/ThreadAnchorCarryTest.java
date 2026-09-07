package com.specforge.discussion.service;

import com.specforge.review.AnchorState;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ThreadAnchorCarryTest {

    @Test
    void anUnrelatedEditLeavesACurrentThreadCurrent() {
        assertThat(ThreadAnchorCarry.next(AnchorState.CURRENT, AnchorState.CURRENT)).isEqualTo(AnchorState.CURRENT);
    }

    @Test
    void anEditToItsSectionMakesACurrentThreadStale() {
        assertThat(ThreadAnchorCarry.next(AnchorState.CURRENT, AnchorState.STALE)).isEqualTo(AnchorState.STALE);
    }

    @Test
    void itsSectionDisappearingOrphansIt() {
        assertThat(ThreadAnchorCarry.next(AnchorState.CURRENT, AnchorState.ORPHANED)).isEqualTo(AnchorState.ORPHANED);
    }

    @Test
    void aStaleThreadStaysStaleEvenWhenTheNextHopLeavesItsSectionUntouched() {
        assertThat(ThreadAnchorCarry.next(AnchorState.STALE, AnchorState.CURRENT)).isEqualTo(AnchorState.STALE);
    }

    @Test
    void aStaleThreadOrphansWhenItsSectionThenDisappears() {
        assertThat(ThreadAnchorCarry.next(AnchorState.STALE, AnchorState.ORPHANED)).isEqualTo(AnchorState.ORPHANED);
    }
}
