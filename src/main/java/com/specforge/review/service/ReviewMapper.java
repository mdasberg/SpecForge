package com.specforge.review.service;

import com.specforge.catalog.SpecRefView;
import com.specforge.catalog.SpecSectionRange;
import com.specforge.catalog.SpecText;
import com.specforge.catalog.SpecVersionContent;
import com.specforge.platform.api.dto.PullRequestRef;
import com.specforge.platform.api.dto.Review;
import com.specforge.platform.api.dto.ReviewSide;
import com.specforge.platform.api.dto.ReviewState;
import com.specforge.platform.api.dto.ReviewSummary;
import com.specforge.platform.api.dto.SpecOutlineSection;
import com.specforge.platform.api.dto.SpecRef;
import com.specforge.platform.api.dto.SpecStatus;
import com.specforge.review.entity.ReviewEntity;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

/** Entity to contract. It is the only place a review's wire shape is decided. */
@Component
class ReviewMapper {

    ReviewSummary summary(final ReviewEntity review, final SpecRefView spec) {
        final ReviewSummary summary = new ReviewSummary(
                review.id(),
                ref(spec),
                ReviewState.fromValue(review.state().name()),
                // The base's author and timestamp are left out of a list row on purpose: filling
                // them would mean reading the catalogue once per row to render text nobody reads in
                // a list. The detail response has them.
                new ReviewSide("v" + review.baseVersionOrdinal(), review.baseContentSha())
                        .ordinal(review.baseVersionOrdinal()),
                headSide(review),
                at(review.openedAt()),
                at(review.updatedAt()));
        summary.setPullRequest(pullRequest(review));
        summary.setOpenedBy(review.openedBy());
        return summary;
    }

    Review detail(
            final ReviewEntity review,
            final SpecRefView spec,
            final SpecVersionContent base,
            final SpecText head) {
        final Review detail = new Review(
                review.id(),
                ref(spec),
                ReviewState.fromValue(review.state().name()),
                baseSide(review, base),
                headSide(review),
                head.content(),
                outline(head.sections()),
                at(review.openedAt()),
                at(review.updatedAt()));
        detail.setPullRequest(pullRequest(review));
        detail.setOpenedBy(review.openedBy());
        return detail;
    }

    static ReviewSide side(final SpecVersionContent version) {
        final ReviewSide side = new ReviewSide("v" + version.ordinal(), version.contentSha())
                .ordinal(version.ordinal());
        side.setAuthor(version.author());
        side.setCommitSha(version.commitSha());
        side.setCreatedAt(at(version.createdAt()));
        return side;
    }

    static List<SpecOutlineSection> outline(final List<SpecSectionRange> sections) {
        return sections.stream()
                .map(section -> new SpecOutlineSection(
                        section.anchorKey(), section.heading(), section.level(), section.ordinal()))
                .toList();
    }

    private ReviewSide baseSide(final ReviewEntity review, final SpecVersionContent base) {
        return base == null
                ? new ReviewSide("v" + review.baseVersionOrdinal(), review.baseContentSha())
                        .ordinal(review.baseVersionOrdinal())
                : side(base);
    }

    /**
     * A pull request head is labelled by its number rather than by a version, because it is not one:
     * calling it "v4" would promise a version the repository has never accepted.
     */
    ReviewSide headSide(final ReviewEntity review) {
        final String label = review.headVersionOrdinal() == null
                ? "#" + review.pullRequestNumber()
                : "v" + review.headVersionOrdinal();
        final ReviewSide side = new ReviewSide(label, review.headContentSha())
                .ordinal(review.headVersionOrdinal());
        side.setAuthor(review.headAuthor());
        side.setCommitSha(review.headCommitSha());
        side.setCreatedAt(at(review.headCreatedAt()));
        return side;
    }

    private PullRequestRef pullRequest(final ReviewEntity review) {
        return review.pullRequestNumber() == null
                ? null
                : new PullRequestRef(
                        review.repositoryFullName(),
                        review.pullRequestNumber(),
                        review.headCommitSha());
    }

    private SpecRef ref(final SpecRefView spec) {
        return new SpecRef(
                spec.id(),
                spec.title(),
                spec.path(),
                spec.project(),
                spec.repositoryFullName(),
                SpecStatus.fromValue(spec.status().name()));
    }

    private static OffsetDateTime at(final Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
