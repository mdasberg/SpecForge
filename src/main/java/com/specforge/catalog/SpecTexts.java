package com.specforge.catalog;

import com.specforge.catalog.service.MarkdownSections;
import com.specforge.catalog.service.SpecContent;
import java.util.List;

/**
 * The catalogue's normalisation and heading parser, offered to the rest of the product.
 *
 * <p>The diff engine has to split content proposed by a pull request into the same sections an
 * imported version was split into. Doing that with a second markdown parser would let the two
 * disagree about where a section starts, and since the section key is the address a discussion, an
 * agent finding and an approval all use, that disagreement would surface as anchors pointing at the
 * wrong text rather than as a parser bug.
 *
 * <p>It is static and stateless because it touches no database: it is a pure function of the text,
 * which is also what lets a caller test against it without a Spring context.
 */
public final class SpecTexts {

    private SpecTexts() {}

    public static SpecText of(final String rawContent) {
        final String content = SpecContent.normalise(rawContent);
        final List<SpecSectionRange> sections = MarkdownSections.parse(content).stream()
                .map(section -> new SpecSectionRange(
                        section.anchorKey(),
                        section.heading(),
                        section.level(),
                        section.ordinal(),
                        section.startLine(),
                        section.endLine()))
                .toList();
        return new SpecText(content, SpecContent.sha256(content), sections);
    }
}
