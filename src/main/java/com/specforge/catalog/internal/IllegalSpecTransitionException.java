package com.specforge.catalog.internal;

import com.specforge.catalog.SpecStatus;
import java.util.UUID;

/** A lifecycle transition the state machine refuses. Rendered as a 409 by the API. */
class IllegalSpecTransitionException extends RuntimeException {

    IllegalSpecTransitionException(UUID documentId, SpecStatus from, SpecStatus to) {
        super("Specification %s cannot move from %s to %s.".formatted(documentId, from, to));
    }
}
