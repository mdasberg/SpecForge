package com.specforge.review.service;

import com.specforge.catalog.SpecCatalog;
import com.specforge.catalog.SpecRefView;
import com.specforge.catalog.SpecText;
import com.specforge.catalog.SpecTexts;
import com.specforge.catalog.SpecVersionContent;
import com.specforge.platform.api.Cursors;
import com.specforge.platform.api.Problems;
import com.specforge.platform.api.dto.DiffSection;
import com.specforge.platform.api.dto.Review;
import com.specforge.platform.api.dto.ReviewList;
import com.specforge.platform.api.dto.ReviewRequest;
import com.specforge.platform.api.dto.ReviewSummary;
import com.specforge.platform.api.dto.SpecDiff;
import com.specforge.repository.ProposalClosed;
import com.specforge.repository.ProposedSpec;
import com.specforge.repository.SpecChangeProposed;
import com.specforge.review.ReviewHeadAdvanced;
import com.specforge.review.entity.ReviewEntity;
import com.specforge.review.entity.ReviewState;
import com.specforge.review.repository.ReviewRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reviews: opening one, following a pull request's head, and serving the review screen.
 *
 * <p>A review is per specification. A pull request touching four specifications opens four reviews,
 * because a reviewer approves a specification and a single review spanning four of them could not
 * say "approved" honestly about any one. The corollary is that a specification has at most one open
 * review, which the database enforces rather than this class hoping.
 */
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ReviewService {

    /** Matches the contract's own default, so an absent `limit` and an explicit one agree. */
    private static final int DEFAULT_LIMIT = 50;

    private final ReviewRepository reviews;
    private final JdbcClient jdbc;
    private final SpecCatalog catalog;
    private final DiffService diffs;
    private final ReviewMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ReviewList list(
            final com.specforge.platform.api.dto.ReviewState state,
            final UUID specId,
            final Integer limit,
            final String cursor) {
        final int pageSize = limit == null ? DEFAULT_LIMIT : limit;
        final int offset = Cursors.offsetOf(cursor);
        final Map<String, Object> params = new LinkedHashMap<>();
        final StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (state != null) {
            where.append(" AND state = :state");
            params.put("state", ReviewState.valueOf(state.getValue()).name());
        }
        if (specId != null) {
            where.append(" AND document_id = :documentId");
            params.put("documentId", specId);
        }

        final long total = jdbc.sql("SELECT count(*) FROM review" + where)
                .params(params)
                .query(Long.class)
                .single();

        final Map<String, Object> pageParams = new LinkedHashMap<>(params);
        // One id more than asked for: whether a next page exists is then a fact about this result
        // rather than a second count that can disagree with it.
        pageParams.put("limit", pageSize + 1);
        pageParams.put("offset", offset);
        final List<UUID> ids = jdbc
                .sql("SELECT id FROM review" + where + " ORDER BY updated_at DESC, id ASC LIMIT :limit OFFSET :offset")
                .params(pageParams)
                .query((rs, row) -> rs.getObject(1, UUID.class))
                .list();

        final boolean hasMore = ids.size() > pageSize;
        final List<UUID> page = hasMore ? ids.subList(0, pageSize) : ids;
        final Map<UUID, ReviewEntity> loaded = new LinkedHashMap<>();
        reviews.findAllById(page).forEach(review -> loaded.put(review.id(), review));

        final List<ReviewEntity> ordered = page.stream().map(loaded::get).filter(Objects::nonNull).toList();
        final Map<UUID, SpecRefView> specs = catalog.refs(ordered.stream().map(ReviewEntity::documentId).toList());
        final List<ReviewSummary> items = new ArrayList<>(ordered.size());
        for (final ReviewEntity review : ordered) {
            final SpecRefView spec = specs.get(review.documentId());
            if (spec != null) {
                items.add(mapper.summary(review, spec));
            }
        }
        return new ReviewList(items, total).cursor(Cursors.next(offset, pageSize, hasMore));
    }

    public Review get(final UUID reviewId) {
        final ReviewEntity review = require(reviewId);
        return mapper.detail(
                review,
                requireSpec(review.documentId()),
                catalog.version(review.documentId(), review.baseVersionOrdinal()).orElse(null),
                SpecTexts.of(review.headContent()));
    }

    /** The review's diff, which is the Changes tab. */
    public SpecDiff diff(final UUID reviewId) {
        final ReviewEntity review = require(reviewId);
        final SpecVersionContent base = catalog
                .version(review.documentId(), review.baseVersionOrdinal())
                .orElseThrow(() -> Problems.notFound(
                        "Version %d of specification %s is gone.".formatted(
                                review.baseVersionOrdinal(), review.documentId())));
        final SpecText head = SpecTexts.of(review.headContent());
        final List<DiffSection> sections = diffs.attributed(
                diffs.sections(SpecTexts.of(base.content()), head),
                review.headAuthor(),
                review.headCreatedAt());
        return new SpecDiff(
                review.documentId(),
                ReviewMapper.side(base),
                mapper.headSide(review),
                SpecDiffs.summarise(sections),
                sections);
    }

    /**
     * Comparing two versions out of curiosity: it renders, and creates no review, no reviewers and
     * no review history. That is the whole distinction between looking and reviewing.
     */
    public SpecDiff compare(final UUID specId, final int baseOrdinal, final int headOrdinal) {
        final SpecVersionContent base = version(specId, baseOrdinal);
        final SpecVersionContent head = version(specId, headOrdinal);
        final List<DiffSection> sections = diffs.attributed(
                diffs.sections(SpecTexts.of(base.content()), SpecTexts.of(head.content())),
                head.author(),
                head.createdAt());
        return new SpecDiff(
                specId,
                ReviewMapper.side(base),
                ReviewMapper.side(head),
                SpecDiffs.summarise(sections),
                sections);
    }

    /** Opens a review manually, between two imported versions of one specification. */
    @Transactional
    public Review open(final ReviewRequest request, final String openedBy) {
        final UUID specId = request.getSpecId();
        final SpecVersionContent base = version(specId, request.getBaseVersion());
        final SpecVersionContent head = version(specId, request.getHeadVersion());
        if (base.ordinal() == head.ordinal()) {
            throw Problems.unprocessable("A review compares two different versions of a specification.");
        }
        reviews.findByDocumentIdAndState(specId, ReviewState.OPEN).ifPresent(open -> {
            throw Problems.conflict(
                    "Specification %s already has an open review, %s.".formatted(specId, open.id()));
        });

        // Through the lifecycle state machine, so "which moves are legal" stays one table rather
        // than a condition this class repeats.
        catalog.proposeChange(specId);

        final Instant now = clock.instant();
        final ReviewEntity review = new ReviewEntity(
                UUID.randomUUID(), specId, base.ordinal(), base.contentSha(), openedBy, now);
        review.advanceHead(
                head.content(),
                head.contentSha(),
                head.ordinal(),
                head.author(),
                head.commitSha(),
                head.createdAt(),
                now);
        reviews.save(review);
        return mapper.detail(review, requireSpec(specId), base, SpecTexts.of(head.content()));
    }

    /**
     * A change was proposed: one review per specification the pull request touches, and a further
     * push updates that review's head instead of opening a second one.
     */
    @Transactional
    public void proposed(final SpecChangeProposed event) {
        final Instant now = clock.instant();
        for (final ProposedSpec proposed : event.specs()) {
            final SpecVersionContent base = catalog.version(proposed.documentId(), null).orElse(null);
            if (base == null) {
                // Nothing has been imported for this path yet, so there is no base to compare
                // against. The next import creates one; opening a review against nothing would only
                // produce a diff that says the whole file is new.
                continue;
            }
            final SpecText head = SpecTexts.of(proposed.content());
            // Built completely before it is saved. The row's identifier is assigned rather than
            // generated, so Spring Data treats a fresh entity as detached and saves it by merging a
            // copy; saving a half-populated one and filling it in afterwards writes the copy, not
            // the fields.
            final ReviewEntity review = reviews
                    .findByDocumentIdAndState(proposed.documentId(), ReviewState.OPEN)
                    .orElseGet(() -> new ReviewEntity(
                            UUID.randomUUID(), proposed.documentId(), base.ordinal(), base.contentSha(), null, now));
            review.fromPullRequest(event.proposalId(), event.repositoryFullName(), event.pullRequestNumber());
            // The specification is In Review because a review exists, not because a pull request
            // was seen — so the transition happens here, where the review is, and through the
            // lifecycle state machine rather than by setting a column.
            catalog.proposeChange(proposed.documentId());

            final String previousHead = review.headContent();
            final String previousHeadSha = review.headContentSha();
            final boolean moved = review.advanceHead(
                    head.content(),
                    head.contentSha(),
                    null,
                    event.author(),
                    event.headSha(),
                    event.proposedAt(),
                    now);
            reviews.save(review);

            if (moved && previousHead != null) {
                // Anchors are classified here, while both heads are still in hand: SpecForge keeps
                // only the current head, so after this there is nothing left to compare an older
                // anchor against.
                events.publishEvent(new ReviewHeadAdvanced(
                        review.id(),
                        review.documentId(),
                        previousHeadSha,
                        head.contentSha(),
                        AnchorCarry.states(SpecTexts.of(previousHead), head),
                        now));
            }
        }
    }

    /** The pull request is finished, so nothing further will be pushed to the review's head. */
    @Transactional
    public void proposalClosed(final ProposalClosed event) {
        final Instant now = clock.instant();
        for (final ReviewEntity review : reviews.findByProposalId(event.proposalId())) {
            if (review.state() == ReviewState.OPEN) {
                review.close(now);
                reviews.save(review);
            }
        }
    }

    private SpecVersionContent version(final UUID specId, final int ordinal) {
        return catalog
                .version(specId, ordinal)
                .orElseThrow(() -> Problems.notFound(
                        "Specification %s has no version %d.".formatted(specId, ordinal)));
    }

    private ReviewEntity require(final UUID reviewId) {
        return reviews
                .findById(reviewId)
                .orElseThrow(() -> Problems.notFound("No review %s.".formatted(reviewId)));
    }

    private SpecRefView requireSpec(final UUID documentId) {
        final SpecRefView spec = catalog.refs(Set.of(documentId)).get(documentId);
        if (spec == null) {
            throw Problems.notFound("No specification %s.".formatted(documentId));
        }
        return spec;
    }
}
