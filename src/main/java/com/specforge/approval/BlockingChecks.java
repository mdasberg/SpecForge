package com.specforge.approval;

import java.util.UUID;

/**
 * The blocking automated-check state for a review's current head, owned here rather than by
 * {@code automated-review} because that capability does not exist yet — {@code add-automated-review}
 * ships its own implementation of this port later, replacing {@link com.specforge.approval.service.NoBlockingChecks}
 * as the bean the gate reads. Until then every review reports {@link CheckState#NOT_CONFIGURED}, which
 * the gate treats as non-blocking.
 */
public interface BlockingChecks {

    CheckState state(UUID reviewId);
}
