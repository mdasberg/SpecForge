package com.specforge.catalog;

import java.util.List;

/**
 * Content that has been through the catalogue's own normalisation and heading parser.
 *
 * <p>It exists so that a module which has to reason about a specification's structure — the diff
 * engine, above all — asks the catalogue rather than parsing markdown a second time. Two parsers
 * would be free to disagree about where a section starts, and every anchor in the product would
 * then depend on which one happened to answer.
 *
 * @param content the normalised source: LF endings, no trailing whitespace, one closing newline
 * @param contentSha sha256 of {@code content}, the same identity a version is stored under
 */
public record SpecText(String content, String contentSha, List<SpecSectionRange> sections) {}
