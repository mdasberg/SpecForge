package com.specforge.review.api;

import com.specforge.platform.api.dto.Review;
import com.specforge.platform.api.dto.ReviewList;
import com.specforge.platform.api.dto.ReviewRequest;
import com.specforge.platform.api.dto.ReviewState;
import com.specforge.platform.api.dto.SpecDiff;
import com.specforge.platform.Callers;
import com.specforge.platform.api.generated.ReviewApi;
import com.specforge.review.service.ReviewService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * The review capability's HTTP surface, implementing the interface generated from
 * {@code specforge-api.yaml}. There is no mapping here: the service speaks the contract's types and
 * this class only routes.
 */
@RequiredArgsConstructor
@RestController
class ReviewController implements ReviewApi {

    private final ReviewService reviews;
    private final Callers callers;

    @Override
    public ReviewList listReviews(
            final ReviewState state, final UUID specId, final Integer limit, final String cursor) {
        return reviews.list(state, specId, limit, cursor);
    }

    @Override
    public Review getReview(final UUID reviewId) {
        return reviews.get(reviewId);
    }

    @Override
    public SpecDiff getReviewDiff(final UUID reviewId) {
        return reviews.diff(reviewId);
    }

    /**
     * The caller is not an operation parameter — it is the security scheme — so the token is read
     * from the security context rather than from a signature, and it is who the review records as
     * having opened it.
     */
    @Override
    public Review openReview(final ReviewRequest reviewRequest) {
        return reviews.open(reviewRequest, callers.current().displayName());
    }

    @Override
    public SpecDiff compareSpecificationVersions(final UUID specId, final Integer base, final Integer head) {
        return reviews.compare(specId, base, head);
    }
}
