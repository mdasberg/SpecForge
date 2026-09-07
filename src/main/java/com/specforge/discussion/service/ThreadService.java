package com.specforge.discussion.service;

import com.specforge.catalog.SpecSectionRange;
import com.specforge.catalog.SpecText;
import com.specforge.catalog.SpecTexts;
import com.specforge.discussion.entity.CommentEntity;
import com.specforge.discussion.entity.NotificationEntity;
import com.specforge.discussion.entity.NotificationType;
import com.specforge.discussion.entity.ThreadEntity;
import com.specforge.discussion.repository.CommentRepository;
import com.specforge.discussion.repository.NotificationRepository;
import com.specforge.discussion.repository.ThreadRepository;
import com.specforge.platform.ActorKind;
import com.specforge.platform.Caller;
import com.specforge.platform.MemberRef;
import com.specforge.platform.Members;
import com.specforge.platform.api.Problems;
import com.specforge.platform.api.dto.CommentRequest;
import com.specforge.platform.api.dto.Thread;
import com.specforge.platform.api.dto.ThreadList;
import com.specforge.platform.api.dto.ThreadRequest;
import com.specforge.review.AnchorState;
import com.specforge.review.ReviewHead;
import com.specforge.review.ReviewHeadAdvanced;
import com.specforge.review.Reviews;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Threads and their comments: opening, replying, resolving and reopening, and carrying anchors
 * forward when the review's head advances.
 */
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ThreadService {

    // ponytail: fixed rather than configurable — a per-project edit window is not a requirement
    // anyone has asked for; add a setting if that changes.
    private static final Duration EDIT_WINDOW = Duration.ofMinutes(15);

    private final ThreadRepository threads;
    private final CommentRepository comments;
    private final NotificationRepository notifications;
    private final Reviews reviews;
    private final Members members;
    private final ThreadMapper mapper;
    private final Clock clock;

    public ThreadList list(final UUID reviewId) {
        final List<ThreadEntity> entities = threads.findByReviewIdOrderByResolvedAscCreatedAtDesc(reviewId);
        final List<CommentEntity> allComments = entities.isEmpty()
                ? List.of()
                : comments.findByThreadIdInOrderByCreatedAtAsc(entities.stream().map(ThreadEntity::id).toList());
        final Map<UUID, List<CommentEntity>> byThread = allComments.stream()
                .collect(Collectors.groupingBy(CommentEntity::threadId, LinkedHashMap::new, Collectors.toList()));
        final Map<String, MemberRef> actors = resolveActors(entities, allComments);

        int blocking = 0;
        int nonBlocking = 0;
        final List<Thread> items = new java.util.ArrayList<>(entities.size());
        for (final ThreadEntity thread : entities) {
            items.add(mapper.thread(thread, byThread.getOrDefault(thread.id(), List.of()), actors));
            if (!thread.resolved()) {
                if (thread.blocking()) {
                    blocking++;
                } else {
                    nonBlocking++;
                }
            }
        }
        return new ThreadList(items, blocking, nonBlocking);
    }

    /**
     * Anchors against the review's current head: the content and its sha come from the review
     * module, never from the caller, so "written against" cannot be forged.
     */
    @Transactional
    public Thread open(final UUID reviewId, final ThreadRequest request, final Caller caller) {
        final ReviewHead head = reviews.head(reviewId)
                .orElseThrow(() -> Problems.notFound("No review %s.".formatted(reviewId)));
        final SpecText headText = SpecTexts.of(head.content());
        final SpecSectionRange section = headText.sections().stream()
                .filter(candidate -> candidate.anchorKey().equals(request.getSectionKey()))
                .findFirst()
                .orElseThrow(() -> Problems.unprocessable(
                        "Section %s does not exist in %s.".formatted(request.getSectionKey(), head.label())));

        Integer rangeStart = null;
        Integer rangeEnd = null;
        final String quotedText;
        if (request.getRange() != null) {
            final int start = request.getRange().getStartOffset();
            final int end = request.getRange().getEndOffset();
            if (start < 0 || end <= start || end > headText.content().length()) {
                throw Problems.badRequest("range is out of bounds for the head content.");
            }
            final String actual = headText.content().substring(start, end);
            if (!actual.equals(request.getQuotedText())) {
                throw Problems.unprocessable("quotedText does not match the head content at that range.");
            }
            rangeStart = start;
            rangeEnd = end;
            quotedText = actual;
        } else {
            // Whole-section anchor: the heading is what a stale or orphaned render shows, since
            // there is no selected phrase to fall back on.
            quotedText = section.heading();
        }

        final Instant now = clock.instant();
        final ThreadEntity thread = new ThreadEntity(
                UUID.randomUUID(),
                reviewId,
                head.documentId(),
                section.anchorKey(),
                rangeStart,
                rangeEnd,
                quotedText,
                head.contentSha(),
                head.label(),
                caller.subjectId(),
                caller.actorKind(),
                now);
        threads.save(thread);
        final CommentEntity opening = addComment(thread, request.getBody(), caller, now, List.of());

        return mapper.thread(thread, List.of(opening), resolveActors(List.of(thread), List.of(opening)));
    }

    @Transactional
    public Thread reply(final UUID threadId, final CommentRequest request, final Caller caller) {
        final ThreadEntity thread = require(threadId);
        final List<CommentEntity> existing = comments.findByThreadIdOrderByCreatedAtAsc(threadId);
        final Instant now = clock.instant();
        addComment(thread, request.getBody(), caller, now, existing);
        thread.touch(now);
        threads.save(thread);

        final List<CommentEntity> all = comments.findByThreadIdOrderByCreatedAtAsc(threadId);
        return mapper.thread(thread, all, resolveActors(List.of(thread), all));
    }

    @Transactional
    public Thread editComment(
            final UUID threadId, final UUID commentId, final CommentRequest request, final Caller caller) {
        final ThreadEntity thread = require(threadId);
        final CommentEntity comment = comments.findById(commentId)
                .filter(candidate -> candidate.threadId().equals(threadId))
                .orElseThrow(() -> Problems.notFound("No comment %s on thread %s.".formatted(commentId, threadId)));
        if (!comment.author().equals(caller.subjectId())) {
            throw Problems.conflict("Only a comment's own author may edit it.");
        }
        final Instant now = clock.instant();
        if (comment.createdAt().plus(EDIT_WINDOW).isBefore(now)) {
            throw Problems.conflict("The edit window for comment %s has passed.".formatted(commentId));
        }

        final Set<String> previouslyMentioned = comment.mentionedSubjectIds();
        final Set<String> mentionedSubjectIds = new LinkedHashSet<>();
        for (final String handle : MentionParser.handles(request.getBody())) {
            members.byHandle(handle).ifPresent(member -> mentionedSubjectIds.add(member.subjectId()));
        }
        mentionedSubjectIds.remove(caller.subjectId());
        comment.edit(request.getBody(), mentionedSubjectIds, now);
        comments.save(comment);

        // Only the newly added mentions notify — someone already mentioned in the original body was
        // already told once, and re-notifying on every edit would make the mention noisy rather than
        // useful.
        for (final String subjectId : mentionedSubjectIds) {
            if (!previouslyMentioned.contains(subjectId)) {
                notify(NotificationType.MENTION, thread, comment, caller, subjectId, now);
            }
        }

        final List<CommentEntity> all = comments.findByThreadIdOrderByCreatedAtAsc(threadId);
        return mapper.thread(thread, all, resolveActors(List.of(thread), all));
    }

    @Transactional
    public Thread resolve(final UUID threadId, final Caller caller) {
        final ThreadEntity thread = require(threadId);
        requireHuman(caller);
        if (thread.resolved()) {
            throw Problems.conflict("Thread %s is already resolved.".formatted(threadId));
        }
        thread.resolve(caller.subjectId(), clock.instant());
        threads.save(thread);
        final List<CommentEntity> all = comments.findByThreadIdOrderByCreatedAtAsc(threadId);
        return mapper.thread(thread, all, resolveActors(List.of(thread), all));
    }

    @Transactional
    public Thread reopen(final UUID threadId, final Caller caller) {
        final ThreadEntity thread = require(threadId);
        requireHuman(caller);
        if (!thread.resolved()) {
            throw Problems.conflict("Thread %s is not resolved.".formatted(threadId));
        }
        thread.reopen(caller.subjectId(), clock.instant());
        threads.save(thread);
        final List<CommentEntity> all = comments.findByThreadIdOrderByCreatedAtAsc(threadId);
        return mapper.thread(thread, all, resolveActors(List.of(thread), all));
    }

    /**
     * Carries every still-live anchor of the review forward. Once orphaned a thread stays orphaned,
     * and once stale it stays stale even if a later head leaves its section untouched — a thread
     * only ever drifts further from the text it quoted, never back toward it.
     */
    @Transactional
    public void onHeadAdvanced(final ReviewHeadAdvanced event) {
        final Instant now = clock.instant();
        for (final ThreadEntity thread : threads.findByReviewIdAndAnchorStateNot(event.reviewId(), AnchorState.ORPHANED)) {
            final AnchorState computed = event.sections().get(thread.sectionKey());
            if (computed == null) {
                continue;
            }
            final AnchorState next = ThreadAnchorCarry.next(thread.anchorState(), computed);
            if (next != thread.anchorState()) {
                thread.carry(next, now);
                threads.save(thread);
            }
        }
    }

    /**
     * Posts one comment and raises its notifications: a mention notifies whoever it resolved to,
     * and a reply also notifies the thread's other participants, once each, mentions taking
     * priority over a redundant reply notification for the same comment.
     */
    private CommentEntity addComment(
            final ThreadEntity thread, final String body, final Caller caller, final Instant now,
            final List<CommentEntity> existingComments) {
        final Set<String> mentionedSubjectIds = new LinkedHashSet<>();
        for (final String handle : MentionParser.handles(body)) {
            members.byHandle(handle).ifPresent(member -> mentionedSubjectIds.add(member.subjectId()));
        }
        mentionedSubjectIds.remove(caller.subjectId());

        final CommentEntity comment = new CommentEntity(
                UUID.randomUUID(), thread.id(), caller.subjectId(), caller.actorKind(), body, mentionedSubjectIds, now);
        comments.save(comment);

        final Set<String> notified = new HashSet<>();
        for (final String subjectId : mentionedSubjectIds) {
            notify(NotificationType.MENTION, thread, comment, caller, subjectId, now);
            notified.add(subjectId);
        }
        if (!existingComments.isEmpty()) {
            final Set<String> participants = existingComments.stream()
                    .map(CommentEntity::author)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            participants.remove(caller.subjectId());
            participants.removeAll(notified);
            for (final String subjectId : participants) {
                notify(NotificationType.REPLY, thread, comment, caller, subjectId, now);
            }
        }
        return comment;
    }

    private void notify(
            final NotificationType type, final ThreadEntity thread, final CommentEntity comment,
            final Caller caller, final String recipientSubjectId, final Instant now) {
        notifications.save(new NotificationEntity(
                UUID.randomUUID(), recipientSubjectId, type, thread.reviewId(), thread.id(), comment.id(),
                caller.subjectId(), now));
    }

    private Map<String, MemberRef> resolveActors(final List<ThreadEntity> threadRows, final List<CommentEntity> commentRows) {
        final Set<String> subjectIds = new LinkedHashSet<>();
        for (final ThreadEntity thread : threadRows) {
            subjectIds.add(thread.openedBy());
            if (thread.resolvedBy() != null) {
                subjectIds.add(thread.resolvedBy());
            }
            if (thread.reopenedBy() != null) {
                subjectIds.add(thread.reopenedBy());
            }
        }
        for (final CommentEntity comment : commentRows) {
            subjectIds.add(comment.author());
            subjectIds.addAll(comment.mentionedSubjectIds());
        }
        return members.byIds(subjectIds);
    }

    private ThreadEntity require(final UUID threadId) {
        return threads.findById(threadId).orElseThrow(() -> Problems.notFound("No thread %s.".formatted(threadId)));
    }

    /**
     * Resolving a conversation is settling it, and an approval waits on it being settled — so it is
     * a human's act for the same reason approving is. An agent that could resolve the thread its own
     * finding provoked would be clearing the gate it was supposed to be an input to.
     */
    private static void requireHuman(final Caller caller) {
        if (caller.actorKind() != ActorKind.HUMAN) {
            throw Problems.conflict("An agent identity may not resolve or reopen a discussion.");
        }
    }
}
