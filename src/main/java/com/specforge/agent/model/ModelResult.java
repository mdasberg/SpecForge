package com.specforge.agent.model;

import java.util.List;

/**
 * The model's answer, with the identity of the model that gave it — which is the first thing asked
 * for when a finding turns out to be wrong, and the reason a rerun is an explicit act.
 */
public record ModelResult(String modelIdentity, List<ModelFinding> findings) {}
