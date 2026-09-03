## Context

Specs are files in someone else's repository. SpecForge has to mirror them faithfully, detect
change, and anchor human discussion to text that keeps moving — without ever becoming a second
source of truth for the spec content itself.

## Goals / Non-Goals

**Goals:**
- Connecting a repository is honest about what will happen before it happens.
- A version is created exactly when content actually changed.
- Anchors are stable enough that a discussion survives an unrelated edit elsewhere in the file.

**Non-Goals:**
- Editing specs in SpecForge.
- Forges other than GitHub.
- Deriving requirements semantically (that is `add-automated-review`'s work).

## Decisions

- **Read-only mirror.** SpecForge never pushes, comments or opens PRs on the repository. Reviews,
  threads and approvals are SpecForge state; the only outbound write is a commit status on a PR,
  which is a signal, not content. This keeps the git history authoritative and makes the product
  safe to point at a repository you do not own.
- **Sync on pull request is the default.** A PR touching a matched file is the moment a team wants
  review, and approvals reporting back as a check meets them where they already gate merges.
  Push-to-branch and manual re-import stay available for teams whose specs land directly on the
  default branch.
- **Versions are content-addressed.** A version's identity is the sha256 of the normalised file
  content. Re-importing an unchanged file is a no-op even when the commit sha differs, which keeps
  the version timeline meaningful rather than mirroring commit noise.
- **Section keys are heading-slug plus ordinal, not line numbers.** Line numbers move on every
  edit; heading slugs move only when the heading itself changes. A renamed or removed heading
  orphans its anchors deliberately — the threads then render as outdated against the quoted
  original, which is truthful, instead of silently reattaching to the wrong text.
- **GitHub App, not a personal access token.** Installation-scoped access, per-repository grants,
  and revocation the team controls. A revoked installation degrades the connection rather than
  deleting imported history.
- **Import runs are recorded.** Each scan or import is a row with counts and per-file outcomes, so
  "why is this spec not here" is answerable without re-running anything.

## Risks / Trade-offs

- Heading-based anchors break on heading rewrites. Accepted, and surfaced: the alternative
  (fuzzy text matching) reattaches wrongly and quietly, which is worse in a review tool.
- Normalising content before hashing (line endings, trailing whitespace) risks hiding a
  whitespace-only change. Accepted: whitespace-only diffs are noise in a spec review.
- A repository with thousands of matched files makes the wizard's scan slow. The scan is
  asynchronous with progress, and the glob is the user's lever.
