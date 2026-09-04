## Context

A spec diff is not a code diff. Reviewers reason in sections ("the API contract changed"), not in
line ranges, and a spec's headings are its structure. Everything else in the product — threads,
agent findings, approvals, the audit trail — hangs off whatever addressing scheme the diff uses, so
this is the load-bearing decision in the plan.

## Goals / Non-Goals

**Goals:**
- Section-level answers first, line-level detail on demand.
- One computed diff shared by the UI, agents and the audit record.
- A thread written against version 2 still makes sense at version 4.

**Non-Goals:**
- Three-way or merge-conflict resolution — SpecForge never merges anything.
- Semantic diffing ("this requirement weakened"); that is an agent's job.
- Editing during review.

## Decisions

- **Diff is computed on the server and cached by (base hash, head hash).** Versions are immutable,
  so a diff is a pure function of two hashes and can be cached forever. It also means the reviewer,
  the agent and the audit log all cite the identical diff.
- **Two-level diff: sections classified, then lines within a section.** A section is added, removed,
  modified or unchanged by comparing its key set and content hash; a modified section gets a
  line diff with word-level highlighting. This is what makes the jump list possible.
- **Inline and side-by-side are two renderings of one payload.** Both modes are derived client-side
  from the same section-and-line structure, so they can never disagree, and switching modes never
  refetches.
- **Anchors are carried, not migrated.** When a review's head advances, anchors on sections whose key
  and content hash survive are carried across unchanged; anchors on a section whose content changed
  stay attached but are flagged `stale`; anchors whose section key is gone are flagged `orphaned` and
  render against the quoted original. No fuzzy reattachment.
- **A review is per specification, not per pull request.** A pull request touching four specs opens
  four reviews. Reviewers approve specifications, and approval rules are per project and per spec —
  a single review spanning specs could not express "approved" honestly.
- **Manual comparison is not a review.** Comparing v1 with v3 out of curiosity creates no review, no
  reviewers and no audit review event; it just renders a diff.

## Risks / Trade-offs

- Four reviews for one pull request can feel like ceremony. Mitigated by the commit status
  aggregating them and the dashboard grouping by pull request; not by merging reviews.
- Section-hash comparison flags a whole section as modified for a one-word change. That is the
  intended granularity for navigation; the line diff inside carries the detail.
- Caching diffs forever grows a table monotonically. Cheap rows, and content-addressed, so they
  deduplicate across reviews of the same content.
