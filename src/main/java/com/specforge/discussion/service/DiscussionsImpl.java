package com.specforge.discussion.service;

import com.specforge.discussion.Discussions;
import com.specforge.discussion.repository.ThreadRepository;
import com.specforge.platform.ActorKind;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
class DiscussionsImpl implements Discussions {

    private final ThreadRepository threads;

    @Override
    public int unresolvedBlockingCount(final UUID reviewId) {
        return threads.countByReviewIdAndResolvedFalseAndOpenerActorKind(reviewId, ActorKind.HUMAN);
    }
}
