package com.specforge.catalog;

/**
 * One heading and the lines it covers, as another capability sees it. The anchor key is the heading
 * slug plus that slug's ordinal in the document — the same address a discussion or a diff uses, so
 * there is one addressing scheme in the product rather than one per module that parses markdown.
 *
 * @param startLine 1-based line of the heading itself
 * @param endLine 1-based last line the section covers, nested subsections included
 */
public record SpecSectionRange(
        String anchorKey, String heading, int level, int ordinal, int startLine, int endLine) {}
