package com.specforge.discussion.service;

import com.specforge.discussion.entity.CommentEntity;
import com.specforge.discussion.entity.ThreadEntity;
import com.specforge.platform.ActorKind;
import com.specforge.platform.MemberRef;
import com.specforge.platform.api.dto.ActorRef;
import com.specforge.platform.api.dto.AnchorState;
import com.specforge.platform.api.dto.Comment;
import com.specforge.platform.api.dto.TextRange;
import com.specforge.platform.api.dto.Thread;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Entity to contract. It is the only place a thread's or a comment's wire shape is decided. */
@Component
class ThreadMapper {

    Thread thread(final ThreadEntity thread, final List<CommentEntity> comments, final Map<String, MemberRef> actors) {
        final Thread dto = new Thread(
                thread.id(),
                thread.reviewId(),
                thread.sectionKey(),
                thread.quotedText(),
                thread.anchorVersionLabel(),
                AnchorState.fromValue(thread.anchorState().name()),
                ref(thread.openedBy(), thread.openerActorKind(), actors),
                thread.blocking(),
                thread.resolved(),
                comments.stream().map(comment -> comment(comment, actors)).toList(),
                at(thread.createdAt()),
                at(thread.updatedAt()));
        dto.setRange(range(thread));
        if (thread.resolvedBy() != null) {
            dto.setResolvedBy(ref(thread.resolvedBy(), actors));
            dto.setResolvedAt(at(thread.resolvedAt()));
        }
        if (thread.reopenedBy() != null) {
            dto.setReopenedBy(ref(thread.reopenedBy(), actors));
            dto.setReopenedAt(at(thread.reopenedAt()));
        }
        return dto;
    }

    private Comment comment(final CommentEntity comment, final Map<String, MemberRef> actors) {
        final Comment dto = new Comment(
                comment.id(),
                comment.threadId(),
                ref(comment.author(), comment.actorKind(), actors),
                comment.body(),
                comment.mentionedSubjectIds().stream()
                        .map(subjectId -> actors.get(subjectId))
                        .filter(java.util.Objects::nonNull)
                        .map(MemberRef::handle)
                        .toList(),
                at(comment.createdAt()));
        dto.setEditedAt(at(comment.editedAt()));
        return dto;
    }

    private static TextRange range(final ThreadEntity thread) {
        return thread.rangeStart() == null ? null : new TextRange(thread.rangeStart(), thread.rangeEnd());
    }

    /** Resolves through the identity mirror when it can, and falls back to what the row itself knows. */
    private static ActorRef ref(final String subjectId, final ActorKind actorKind, final Map<String, MemberRef> actors) {
        final MemberRef member = actors.get(subjectId);
        return member == null
                ? new ActorRef(subjectId, subjectId, dto(actorKind))
                : new ActorRef(member.subjectId(), member.displayName(), dto(member.actorKind()))
                        .handle(member.handle())
                        .avatarUrl(member.avatarUrl());
    }

    private static ActorRef ref(final String subjectId, final Map<String, MemberRef> actors) {
        final MemberRef member = actors.get(subjectId);
        return member == null
                ? new ActorRef(subjectId, subjectId, dto(ActorKind.HUMAN))
                : new ActorRef(member.subjectId(), member.displayName(), dto(member.actorKind()))
                        .handle(member.handle())
                        .avatarUrl(member.avatarUrl());
    }

    /** The domain and contract enums are deliberately separate types; see MeController. */
    private static com.specforge.platform.api.dto.ActorKind dto(final ActorKind actorKind) {
        return com.specforge.platform.api.dto.ActorKind.fromValue(actorKind.name());
    }

    private static OffsetDateTime at(final Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
