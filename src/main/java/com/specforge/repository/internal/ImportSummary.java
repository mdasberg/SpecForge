package com.specforge.repository.internal;

/** What an import run did, counted per outcome. */
record ImportSummary(int imported, int unchanged, int skipped, int failed) {}
