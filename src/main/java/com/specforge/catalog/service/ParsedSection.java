package com.specforge.catalog.service;

/**
 * One heading found in a version's content.
 *
 * @param anchorKey heading slug plus its occurrence ordinal, the target discussions address
 * @param parentIndex index of the enclosing section in the parsed list, or -1 at the top level
 * @param startLine 1-based line of the heading itself
 * @param endLine 1-based last line the section covers
 */
public record ParsedSection(
        String anchorKey, String heading, int level, int ordinal, int startLine, int endLine, int parentIndex) {}
