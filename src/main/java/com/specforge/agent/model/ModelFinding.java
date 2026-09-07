package com.specforge.agent.model;

/**
 * One finding as the model returned it. Severity is a string here, not SpecForge's enum: this record
 * mirrors what the other side said, and the service decides what an unrecognised value means rather
 * than the deserialiser refusing the whole answer.
 */
public record ModelFinding(
        String sectionKey, String severity, String title, String body, String proposedText) {}
