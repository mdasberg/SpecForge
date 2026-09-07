package com.specforge.review.service;

import com.specforge.catalog.SpecCatalog;
import com.specforge.catalog.SpecTexts;
import com.specforge.catalog.SpecVersionContent;
import com.specforge.platform.api.dto.DiffChange;
import com.specforge.platform.api.dto.DiffSection;
import com.specforge.review.ReviewComparison;
import com.specforge.review.ReviewHead;
import com.specforge.review.ReviewRef;
import com.specforge.review.Reviews;
import com.specforge.review.entity.ReviewEntity;
import com.specforge.review.entity.ReviewState;
import com.specforge.review.repository.ReviewRepository;
import java.util.List;
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
    private final SpecCatalog catalog;
    private final DiffService diffs;

    @Override
    public Optional<ReviewHead> head(final UUID reviewId) {
        return reviews.findById(reviewId).map(ReviewsImpl::head);
    }

    @Override
    public Optional<ReviewRef> ref(final UUID reviewId) {
        return reviews.findById(reviewId).map(review -> new ReviewRef(
                review.id(), review.documentId(), review.state() == ReviewState.OPEN, review.proposalId()));
    }

    @Override
    public Optional<ReviewComparison> comparison(final UUID reviewId) {
        return reviews.findById(reviewId).flatMap(review -> catalog
                .version(review.documentId(), review.baseVersionOrdinal())
                .map(base -> comparison(review, base)));
    }

    private ReviewComparison comparison(final ReviewEntity review, final SpecVersionContent base) {
        // The same cached diff the Changes tab renders, so a check and a reviewer are looking at
        // one classification of one pair of contents.
        final List<DiffSection> sections = diffs.sections(
                SpecTexts.of(base.content()), SpecTexts.of(review.headContent()));
        final List<String> changed = sections.stream()
                .filter(section -> section.getChange() != DiffChange.UNCHANGED)
                // The preamble carries the empty key, which is not a section anything can anchor to.
                .filter(section -> !section.getAnchorKey().isEmpty())
                .map(DiffSection::getAnchorKey)
                .toList();
        return new ReviewComparison(
                review.id(),
                review.documentId(),
                base.content(),
                review.headContent(),
                review.headContentSha(),
                label(review),
                changed);
    }

    /** The same label {@link ReviewMapper#headSide} renders — a pull-request head has no ordinal. */
    private static ReviewHead head(final ReviewEntity review) {
        return new ReviewHead(
                review.id(), review.documentId(), review.headContent(), review.headContentSha(), label(review));
    }

    private static String label(final ReviewEntity review) {
        return review.headVersionOrdinal() == null
                ? "#" + review.pullRequestNumber()
                : "v" + review.headVersionOrdinal();
    }
}
