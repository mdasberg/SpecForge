## Why

Specifications live in git, not in SpecForge. Nothing can be reviewed until a repository holding
them has been connected, its spec files discovered and imported, and subsequent commits turned
into new immutable versions. The prototype makes this the product's front door: a three-step
wizard (Repository → Specifications → Project) that scans before it commits, so a team sees
exactly which files will import as specs, which are change proposals, and which will not parse.

This change delivers ingestion and the spec document model. It deliberately stops short of
browsing (`add-spec-catalog`) and reviewing (`add-spec-review`).

## What Changes

- Connect a GitHub repository through a GitHub App installation: choose from the granted
  repositories, confirm branch, path glob (default `openspec/specs/**/spec.md`) and format.
- Dry-run scan before connecting, classifying every matched file as importable spec, change
  proposal or unparsable, with counts and per-file reasons.
- Import creates spec documents with metadata (project, domain, team, owner, tags, path) and an
  initial immutable version.
- Content-addressed versions: re-importing identical content is a no-op; changed content creates
  the next version with author, commit sha and timestamp.
- Parse the markdown into a section tree with stable anchor keys — the anchor target that
  discussions and diffs later depend on.
- Lifecycle status per spec document (Draft → In Review → Changes Requested → Approved →
  Implemented) with the legal transitions enforced in one place.
- Three sync modes: on pull request (default), on push, manual. PR sync is what makes a review
  appear; the review itself arrives in `add-spec-review`, so this change records the trigger and
  emits the event.
- SpecForge is read-only on the repository: no capability may write spec content back to git.

## Capabilities

### New Capabilities
- `spec-repository`: repository connections, scanning, import, sync modes and the outbound
  status report to the source forge.
- `spec-document`: the spec document itself — identity, metadata, immutable versions, section
  structure and lifecycle status.

### Modified Capabilities
(none)

## Impact

- New modules `repository` and `catalog` (spec-document lives in `catalog`); new tables for
  connections, spec documents, spec versions, sections and import runs.
- Outbound integration with the GitHub API (App installation tokens, contents, pull requests,
  commit statuses) and an inbound webhook endpoint — the first external surface, so it needs
  signature verification and replay protection.
- GitLab and Bitbucket are explicitly out of scope; the forge port is shaped so they can be added
  without touching import or versioning.
- Depends on: `add-project-skeleton`.
