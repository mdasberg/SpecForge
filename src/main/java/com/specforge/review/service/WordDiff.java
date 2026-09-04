package com.specforge.review.service;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.specforge.platform.api.dto.WordRange;
import java.util.ArrayList;
import java.util.List;

/**
 * The words that differ between two lines that were matched to each other, as character ranges into
 * each line.
 *
 * <p>Ranges rather than a re-tokenised copy of the line: the client already has the text, and
 * handing it offsets means the rendered line and the highlight cannot drift apart. The same Myers
 * diff that produced the line pairing is reused over word tokens, so "changed" means the same thing
 * at both levels.
 *
 * <p>Whitespace runs are tokens of their own rather than being attached to the word beside them.
 * That is what lets two neighbouring changed words merge into one contiguous highlight instead of
 * two with an unhighlighted gap between them.
 */
final class WordDiff {

    /** Character ranges to highlight on each side of a matched pair of lines. */
    record Ranges(List<WordRange> base, List<WordRange> head) {}

    private record Token(String text, int start, int end) {}

    private WordDiff() {}

    static Ranges of(final String base, final String head) {
        final List<Token> baseTokens = tokens(base);
        final List<Token> headTokens = tokens(head);
        final List<WordRange> baseRanges = new ArrayList<>();
        final List<WordRange> headRanges = new ArrayList<>();

        for (final AbstractDelta<String> delta : DiffUtils
                .diff(texts(baseTokens), texts(headTokens))
                .getDeltas()) {
            collect(delta.getSource(), baseTokens, baseRanges);
            collect(delta.getTarget(), headTokens, headRanges);
        }
        return new Ranges(merged(baseRanges, base), merged(headRanges, head));
    }

    private static void collect(
            final Chunk<String> chunk, final List<Token> tokens, final List<WordRange> into) {
        for (int i = chunk.getPosition(); i < chunk.getPosition() + chunk.size(); i++) {
            into.add(new WordRange(tokens.get(i).start(), tokens.get(i).end()));
        }
    }

    private static List<String> texts(final List<Token> tokens) {
        return tokens.stream().map(Token::text).toList();
    }

    /** A token is a run of letters and digits, or a run of everything else. */
    private static List<Token> tokens(final String line) {
        final List<Token> tokens = new ArrayList<>();
        int start = 0;
        while (start < line.length()) {
            final boolean word = Character.isLetterOrDigit(line.charAt(start));
            int end = start + 1;
            while (end < line.length() && Character.isLetterOrDigit(line.charAt(end)) == word) {
                end++;
            }
            tokens.add(new Token(line.substring(start, end), start, end));
            start = end;
        }
        return tokens;
    }

    /**
     * Ranges that touch, or that are separated by nothing but whitespace, become one: two adjacent
     * changed words are one changed phrase to a reader, and highlighting them separately draws a
     * seam through the middle of it. The result is then trimmed of surrounding whitespace, so the
     * highlight ends on the last changed character rather than on the space after it.
     */
    private static List<WordRange> merged(final List<WordRange> ranges, final String line) {
        final List<WordRange> merged = new ArrayList<>(ranges.size());
        for (final WordRange range : ranges) {
            final WordRange previous = merged.isEmpty() ? null : merged.getLast();
            if (previous != null && line.substring(previous.getEnd(), range.getStart()).isBlank()) {
                previous.setEnd(Math.max(previous.getEnd(), range.getEnd()));
            } else {
                merged.add(new WordRange(range.getStart(), range.getEnd()));
            }
        }
        final List<WordRange> trimmed = new ArrayList<>(merged.size());
        for (final WordRange range : merged) {
            int start = range.getStart();
            int end = range.getEnd();
            while (start < end && Character.isWhitespace(line.charAt(start))) {
                start++;
            }
            while (end > start && Character.isWhitespace(line.charAt(end - 1))) {
                end--;
            }
            if (start < end) {
                trimmed.add(new WordRange(start, end));
            }
        }
        return trimmed;
    }
}
