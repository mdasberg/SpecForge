package com.specforge.repository.service;

/** What an import run did, counted per outcome. */
public record ImportSummary(int imported, int unchanged, int skipped, int failed) {}
