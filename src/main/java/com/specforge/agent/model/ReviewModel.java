package com.specforge.agent.model;

import java.util.Optional;

/**
 * The language model a model-backed check asks about a change.
 *
 * <p>It is a port for the same reason the forge is one: what is on the other side is somebody
 * else's service, with its own credentials, wire format and failure modes, and the review flow
 * should not be able to tell a parser from a model. An empty answer means no model is configured —
 * the check then reports skipped, never passed, because "nobody looked" and "nothing wrong" are
 * different findings.
 */
public interface ReviewModel {

    Optional<ModelResult> review(ModelRequest request);
}
