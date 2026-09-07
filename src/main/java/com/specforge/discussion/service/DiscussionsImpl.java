package com.specforge.discussion.service;

import com.specforge.discussion.Discussions;
import com.specforge.discussion.repository.ThreadRepository;
import com.specforge.platform.ActorKind;
import com.specforge.platform.Caller;
import com.specforge.platform.api.dto.ThreadRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
class DiscussionsImpl implements Discussions {

    private final ThreadRepository threads;
    private final ThreadService threadService;

    @Override
    public int unresolvedBlockingCount(final UUID reviewId) {
        return threads.countByReviewIdAndResolvedFalseAndOpenerActorKind(reviewId, ActorKind.HUMAN);
    }

    /**
     * Through {@link ThreadService} rather than around it: the anchor is validated against the
     * review's live head and the mentions in the body notify, exactly as they would for a thread
     * opened from the Discussions tab. A second write path would be a second set of rules.
     */
    @Override
    @Transactional
    public UUID openOnSection(final UUID reviewId, final String sectionKey, final String body, final Caller caller) {
        return threadService.open(reviewId, new ThreadRequest(sectionKey, body), caller).getId();
    }
}
