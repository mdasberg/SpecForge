package com.specforge.discussion.api;

import com.specforge.discussion.service.NotificationService;
import com.specforge.discussion.service.ThreadService;
import com.specforge.platform.Callers;
import com.specforge.platform.MemberRef;
import com.specforge.platform.Members;
import com.specforge.platform.api.dto.ActorRef;
import com.specforge.platform.api.dto.CommentRequest;
import com.specforge.platform.api.dto.MemberList;
import com.specforge.platform.api.dto.Notification;
import com.specforge.platform.api.dto.NotificationList;
import com.specforge.platform.api.generated.DiscussionsApi;
import com.specforge.platform.api.dto.Thread;
import com.specforge.platform.api.dto.ThreadList;
import com.specforge.platform.api.dto.ThreadRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * The discussion capability's HTTP surface, implementing the interface generated from
 * {@code specforge-api.yaml}. There is no mapping here: the services speak the contract's types and
 * this class only routes.
 */
@RequiredArgsConstructor
@RestController
class DiscussionController implements DiscussionsApi {

    private final ThreadService threads;
    private final NotificationService notifications;
    private final Members members;
    private final Callers callers;

    @Override
    public ThreadList listThreads(final UUID reviewId) {
        return threads.list(reviewId);
    }

    @Override
    public Thread openThread(final UUID reviewId, final ThreadRequest threadRequest) {
        return threads.open(reviewId, threadRequest, callers.current());
    }

    @Override
    public Thread replyToThread(final UUID threadId, final CommentRequest commentRequest) {
        return threads.reply(threadId, commentRequest, callers.current());
    }

    @Override
    public Thread editComment(final UUID threadId, final UUID commentId, final CommentRequest commentRequest) {
        return threads.editComment(threadId, commentId, commentRequest, callers.current());
    }

    @Override
    public Thread resolveThread(final UUID threadId) {
        return threads.resolve(threadId, callers.current());
    }

    @Override
    public Thread reopenThread(final UUID threadId) {
        return threads.reopen(threadId, callers.current());
    }

    @Override
    public NotificationList listNotifications() {
        return notifications.list(callers.current());
    }

    @Override
    public Notification markNotificationRead(final UUID notificationId) {
        return notifications.markRead(notificationId, callers.current());
    }

    @Override
    public MemberList searchMembers(final String query) {
        return new MemberList(members.search(query, 20).stream().map(DiscussionController::ref).toList());
    }

    private static ActorRef ref(final MemberRef member) {
        return new ActorRef(
                        member.subjectId(),
                        member.displayName(),
                        com.specforge.platform.api.dto.ActorKind.fromValue(member.actorKind().name()))
                .handle(member.handle())
                .avatarUrl(member.avatarUrl());
    }
}
