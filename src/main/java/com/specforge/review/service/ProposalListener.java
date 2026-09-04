package com.specforge.review.service;

import com.specforge.repository.ProposalClosed;
import com.specforge.repository.SpecChangeProposed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns what the repository capability observed into reviews.
 *
 * <p>Both listeners run after the publishing transaction commits, not inline with it: the
 * specification's move into review and the proposal's own rows are part of that transaction, and a
 * review opened before they landed would be a review of state no other reader can see. Committing
 * first also means a failure here cannot roll back the synchronisation that succeeded.
 *
 * <p>After commit there is no transaction left, so each listener starts its own.
 */
@RequiredArgsConstructor
@Component
class ProposalListener {

    private final ReviewService reviews;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(final SpecChangeProposed event) {
        reviews.proposed(event);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(final ProposalClosed event) {
        reviews.proposalClosed(event);
    }
}
