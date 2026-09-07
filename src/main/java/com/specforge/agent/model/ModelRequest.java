package com.specforge.agent.model;

/**
 * What the model is asked, and about which text. The content is the review's changed sections
 * only — a full specification on every push would cost the most for the part nobody edited.
 *
 * @param instruction what to look for, phrased for the check being run
 * @param content the changed sections, each preceded by its anchor key so a finding can be anchored
 */
public record ModelRequest(String checkKey, String instruction, String content) {}
