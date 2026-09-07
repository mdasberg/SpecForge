package com.specforge.discussion.service;

import com.specforge.discussion.entity.NotificationEntity;
import com.specforge.discussion.repository.NotificationRepository;
import com.specforge.platform.ActorKind;
import com.specforge.platform.Caller;
import com.specforge.platform.MemberRef;
import com.specforge.platform.Members;
import com.specforge.platform.api.Problems;
import com.specforge.platform.api.dto.ActorRef;
import com.specforge.platform.api.dto.Notification;
import com.specforge.platform.api.dto.NotificationList;
import com.specforge.platform.api.dto.NotificationType;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notifications;
    private final Members members;
    private final Clock clock;

    public NotificationList list(final Caller caller) {
        final List<NotificationEntity> entities = notifications.findByRecipientSubjectIdOrderByCreatedAtDesc(caller.subjectId());
        final Map<String, MemberRef> actors = members.byIds(entities.stream().map(NotificationEntity::actorSubjectId).toList());
        final long unread = notifications.countByRecipientSubjectIdAndReadAtIsNull(caller.subjectId());
        return new NotificationList(entities.stream().map(entity -> dto(entity, actors)).toList(), (int) unread);
    }

    @Transactional
    public Notification markRead(final UUID notificationId, final Caller caller) {
        final NotificationEntity entity = notifications.findById(notificationId)
                .filter(candidate -> candidate.recipientSubjectId().equals(caller.subjectId()))
                .orElseThrow(() -> Problems.notFound("No notification %s.".formatted(notificationId)));
        entity.markRead(clock.instant());
        notifications.save(entity);
        return dto(entity, members.byIds(List.of(entity.actorSubjectId())));
    }

    private static Notification dto(final NotificationEntity entity, final Map<String, MemberRef> actors) {
        final Notification dto = new Notification(
                entity.id(),
                NotificationType.fromValue(entity.type().name()),
                entity.reviewId(),
                entity.threadId(),
                entity.commentId(),
                ref(entity.actorSubjectId(), actors),
                at(entity.createdAt()));
        dto.setReadAt(at(entity.readAt()));
        return dto;
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
