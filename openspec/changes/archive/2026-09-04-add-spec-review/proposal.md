## Why

This is the screen the product exists for. A reviewer opens a changed specification and has to
answer one question quickly: what actually changed, and does it hold up? That means a real diff
between two versions — added, removed and modified sections, previous next to current, who changed
it and when — rendered as a technical document rather than as raw markdown, with every changed
section reachable in one click.

The change-proposed event already arrives from `add-spec-repository`; this change turns it into a
review a human can work through, and it establishes the anchor model that discussions, agent
findings and approvals all attach to.

## What Changes

- A review targets one specification and holds a base version and a head version, opened
  automatically by pull-request synchronisation or manually against any two versions.
- Section-level diff classification: added, removed, modified, unchanged, with word-level
  highlighting inside modified lines.
- Two diff renderings — inline and side-by-side — over the same computed diff.
- Compare any two versions of a specification from outside a review.
- Changed-section navigation: a jump list with per-section change counts.
- A new head version on an open review recomputes the diff, keeps anchors on unchanged sections and
  marks anchors on rewritten sections outdated.

## Capabilities

### New Capabilities
- `spec-review`: reviews as first-class objects, version diffing, diff rendering modes, changed
  section navigation and anchor carry-over across head updates.

### Modified Capabilities
- `spec-document`: none. Reviews reference versions; the document model is unchanged.

## Impact

- New module `review`; tables for reviews and per-review computed diffs (cached by base and head
  content hash, since versions are immutable so a diff never needs invalidating).
- Diff computation is server-side (java-diff-utils) so that the diff the reviewer sees, the diff an
  agent analyses and the diff recorded in the audit trail are the same artefact.
- Frontend: the review screen with its Document and Changes tabs, reusing the catalog's document renderer.
- Depends on: `add-spec-repository`, `add-spec-catalog`.
