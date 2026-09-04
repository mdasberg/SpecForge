## 1. Review object

- [x] 1.1 `review` table: specification, base version, head version, state (open, closed), opened by,
      opened at, pull request reference when applicable.
- [x] 1.2 Consume `SpecChangeProposed`: open a review per changed specification with base = current
      version, head = proposed content; update the head on a repeated proposal instead of opening a second review.
- [x] 1.3 Manual review creation against two chosen versions of one specification.
- [x] 1.4 Move the specification's lifecycle status to In Review when a review opens, through the
      lifecycle state machine.

## 2. Diff computation

- [x] 2.1 Section classification by key set and content hash: added, removed, modified, unchanged.
- [x] 2.2 Line diff within modified sections (java-diff-utils) with word-level ranges inside changed lines.
- [x] 2.3 Cache the computed diff keyed by base and head content hash.
- [x] 2.4 Diff summary: counts of added, removed and modified sections, and total changed lines.

## 3. Anchor carry-over

- [x] 3.1 Anchor resolution service returning, for a section key and version, `current`, `stale` or
      `orphaned` with the original quoted text.
- [x] 3.2 On head update, recompute anchor states for everything attached to the review and emit an event
      so discussions and agent findings can re-render.

## 4. Review screens

- [x] 4.1 Review shell: breadcrumb, title, status badge, version, reviewers summary, tabs
      (Document, Changes, and the tabs later changes fill in).
- [x] 4.2 Changes tab: inline and side-by-side renderings from one payload, mode remembered per user.
- [x] 4.3 Changed-section jump list with per-section counts; selecting one scrolls to it.
- [x] 4.4 Per-changed-section attribution: who changed it and when, from the head version's commit.
- [x] 4.5 Standalone version comparison view reachable from the document's version selector.

## 5. Verification

- [x] 5.1 Tests: a section rename produces one added and one removed section, not one modified;
      a body edit produces one modified section; inline and side-by-side render the same change set.
- [x] 5.2 Test: advancing the head carries anchors on untouched sections and marks the rest stale or orphaned.
