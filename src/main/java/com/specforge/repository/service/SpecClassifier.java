package com.specforge.repository.service;

import com.specforge.repository.entity.Classification;

/**
 * Decides what a matched file actually is before anything imports it. The wizard shows this
 * verdict per file, so "twelve importable, three change proposals, one unparsable" is something an
 * administrator sees before committing to a connection rather than discovering afterwards.
 */
public final class SpecClassifier {

    private SpecClassifier() {}

    public record Verdict(Classification classification, String reason) {

        static Verdict of(Classification classification) {
            return new Verdict(classification, null);
        }
    }

    static Verdict classify(String path, String content) {
        if (isChangeProposal(path)) {
            return Verdict.of(Classification.CHANGE_PROPOSAL);
        }
        if (content == null || content.isBlank()) {
            return new Verdict(Classification.UNPARSABLE, "The file is empty.");
        }
        if (!hasHeading(content)) {
            return new Verdict(
                    Classification.UNPARSABLE,
                    "The file has no heading, so it has no title and nothing a discussion could anchor to.");
        }
        return Verdict.of(Classification.IMPORTABLE_SPEC);
    }

    /** OpenSpec keeps proposed changes under a {@code changes} directory; they are not specifications. */
    private static boolean isChangeProposal(String path) {
        for (String segment : path.split("/")) {
            if (segment.equals("changes")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasHeading(String content) {
        boolean inFence = false;
        for (String line : content.split("\\R")) {
            String trimmed = line.strip();
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                inFence = !inFence;
                continue;
            }
            if (!inFence && trimmed.startsWith("#") && trimmed.contains(" ")) {
                return true;
            }
        }
        return false;
    }
}
