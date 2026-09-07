package com.specforge.approval.api;

import com.specforge.approval.service.ApprovalService;
import com.specforge.platform.Callers;
import com.specforge.platform.api.dto.AddRequiredReviewerRequest;
import com.specforge.platform.api.dto.ApprovalStatus;
import com.specforge.platform.api.dto.VerdictRequest;
import com.specforge.platform.api.generated.ApprovalApi;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * The approval capability's HTTP surface, implementing the interface generated from
 * {@code specforge-api.yaml}. There is no mapping here: the service speaks the contract's types and
 * this class only routes and authorises.
 */
@RequiredArgsConstructor
@RestController
class ApprovalController implements ApprovalApi {

    private final ApprovalService approvals;
    private final Callers callers;

    @Override
    public ApprovalStatus getApprovalStatus(final UUID reviewId) {
        return approvals.status(reviewId, callers.current());
    }

    @Override
    public ApprovalStatus castVerdict(final UUID reviewId, final VerdictRequest verdictRequest) {
        return approvals.castVerdict(reviewId, verdictRequest, callers.current());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ApprovalStatus addRequiredReviewer(final UUID reviewId, final AddRequiredReviewerRequest addRequiredReviewerRequest) {
        return approvals.addRequiredReviewer(reviewId, addRequiredReviewerRequest, callers.current());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ApprovalStatus removeRequiredReviewer(final UUID reviewId, final UUID requiredReviewerId) {
        return approvals.removeRequiredReviewer(reviewId, requiredReviewerId, callers.current());
    }
}
