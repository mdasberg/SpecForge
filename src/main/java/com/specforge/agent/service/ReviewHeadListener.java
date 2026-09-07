package com.specforge.agent.service;

import com.specforge.review.ReviewHeadSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * A review's head was set or advanced, so the checks run against it. Listens after the review
 * capability commits — a run dispatched against a head no other reader can see yet would be a run
 * against nothing — and opens its own transaction, since after commit there is none left.
 */
@RequiredArgsConstructor
@Component
class ReviewHeadListener {

    private final CheckDispatcher dispatcher;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(final ReviewHeadSet event) {
        dispatcher.dispatchAll(event.reviewId());
    }
}
