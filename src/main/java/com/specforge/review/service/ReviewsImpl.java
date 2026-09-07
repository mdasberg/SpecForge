package com.specforge.review.service;

import com.specforge.review.ReviewHead;
import com.specforge.review.Reviews;
import com.specforge.review.entity.ReviewEntity;
import com.specforge.review.repository.ReviewRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
class ReviewsImpl implements Reviews {

    private final ReviewRepository reviews;

    @Override
    public Optional<ReviewHead> head(final UUID reviewId) {
        return reviews.findById(reviewId).map(ReviewsImpl::head);
    }

    /** The same label {@link ReviewMapper#headSide} renders — a pull-request head has no ordinal. */
    private static ReviewHead head(final ReviewEntity review) {
        final String label = review.headVersionOrdinal() == null
                ? "#" + review.pullRequestNumber()
                : "v" + review.headVersionOrdinal();
        return new ReviewHead(review.id(), review.documentId(), review.headContent(), review.headContentSha(), label);
    }
}
