package com.specforge.discussion.service;

import com.specforge.review.ReviewHeadAdvanced;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Carries thread anchors forward after the review capability commits a head advance. Runs in its
 * own transaction, the same way {@code review}'s own listener does: the carry is a consequence of
 * the commit that already happened, not part of it.
 */
@RequiredArgsConstructor
@Component
class ReviewHeadAdvancedListener {

    private final ThreadService threads;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(final ReviewHeadAdvanced event) {
        threads.onHeadAdvanced(event);
    }
}
