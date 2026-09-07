package com.specforge.approval.service;

import com.specforge.review.ReviewHeadAdvanced;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Resets every seat to pending after the review capability commits a head advance, the same way
 * {@code discussion}'s own listener carries thread anchors forward from the same event.
 */
@RequiredArgsConstructor
@Component
class ApprovalHeadAdvancedListener {

    private final ApprovalService approvals;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(final ReviewHeadAdvanced event) {
        approvals.onHeadAdvanced(event);
    }
}
