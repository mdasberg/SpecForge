# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository state

Greenfield. `LICENSE` is the only tracked file (`git ls-files`); there is no application code,
no build file, and therefore no build/lint/test command yet. The implementation plan is the
substance of the project, and it lives in **GitHub issues #1–#10** on `mdasberg/SpecForge`
(label `openspec`). Build commands appear once issue #1 lands.

`openspec/` and `design/` from the 2026-09-03 planning session **are committed**, on the sibling
branch `plan/openspec-implementation` (pushed to origin): `2ace1e5` holds the design prototype and
its tooling, `5b5fe40` the OpenSpec plan for all ten changes. That branch is not an ancestor of
`main`. Take plan or design files from there — `git show plan/openspec-implementation:<path>` —
rather than regenerating them from the GitHub issues.

## Product

SpecForge is a collaborative review and approval platform for software specifications — the review
layer that spec-driven development lacks. Spec lifecycle: Draft → In Review → Changes Requested →
Approved → Implemented, with anchored discussions, version diffs, approval rules, automated/AI
reviewers, ticket creation from a discussion, and a spec→approval→ticket→PR→deployment
traceability chain.

## Stack (decided, not yet built)

Java 25 · Spring Boot 4 · Spring Modulith 2 · PostgreSQL + Liquibase · Keycloak · React 19 + TypeScript
+ Vite. Package base `com.specforge`.

## How work is planned and sequenced

The repo is itself an OpenSpec project (`openspec` CLI 1.5.0, schema `spec-driven`): specs at
`openspec/specs/<capability>/spec.md`, proposals under `openspec/changes/`, driven by the `/opsx:*`
skills (propose → apply → sync → archive). `openspec validate --changes --strict` is the gate.

Ten change proposals, 12 capabilities, ~195 tasks. **Issue N == step N**, and each issue body
carries proposal + design + tasks-as-checkboxes + spec deltas, so implementation can run straight
from the issue:

1. `add-project-skeleton` — app skeleton, Keycloak auth, API conventions, themed shell
2. `add-spec-repository` — connect a GitHub repo, import specs, versions, sync
3. `add-spec-catalog` — browse, filter, search, render
4. `add-spec-review` — reviews, version diff, inline and side-by-side
5. `add-review-discussions` — anchored threads, mentions, resolve
6. `add-review-approval` — reviewers, verdicts, approval rules, the gate
7. `add-automated-review` — check runs, agent findings, accept/dismiss
8. `add-ticket-links` — create a ticket from a comment, tracker adapters
9. `add-audit-trail` — append-only event log, timeline, traceability
10. `add-review-dashboard` — personal review queues, activity feed

Order is a hard dependency chain; do not start step N before N-1 is applied.

## Cross-capability rules (inherited by every capability)

- The connected spec repository is **read-only**. SpecForge imports and renders; comments and
  verdicts live in SpecForge, never written back as file edits.
- Human-vs-agent provenance is an **API field** (`actor_kind`), not a UI convention. Agent
  identities live in the same identity table as humans.
- **Agents never approve.** They may comment, suggest and fail checks.
- Every state change lands in the append-only audit log.

## Auth model (load-bearing)

**Keycloak is both the identity provider and the user store** — SpecForge builds no account,
password or MFA screens. The backend is a resource server validating Keycloak JWTs (`issuer-uri`).
The SPA logs in through public client `specforge-web` (authorization code + PKCE); check runners
authenticate as agents via service accounts on confidential client `specforge-api`. Realm roles
`REVIEWER` / `ARCHITECT` / `ADMIN` are the only role source; the local `user` row is a mirror keyed
by Keycloak subject id, so a rename in Keycloak never orphans a verdict. Dev realm ships as
`keycloak/realm-export.json`, imported by `quay.io/keycloak/keycloak:26.0` with
`start-dev --import-realm` on host port 8081.

## Design decisions already settled (don't re-litigate)

- The HTTP API is **contract-first**. `src/main/resources/openapi/specforge-api.yaml` is the source;
  `org.openapi.generator` generates the server interfaces and DTOs into `build/generated`, and every
  controller implements a generated interface, so drift fails to compile. Never edit generated Java,
  and never add an annotation-scanning document generator — `/api/openapi.json` serves the bundled
  contract itself. The document is split like the other CarePay services: `resources/<area>/` per
  path, `schemas/`, `parameters/`, `responses/`. A resource `$ref`s a shared response directly;
  shared responses are never also listed in the root `components`, or the bundle ships a dangling
  `$ref`.
- **DTOs come from the contract, and the service layer speaks them.** Controllers are pass-throughs
  with no mapping, entity-to-DTO mapping lives in `service/`, and there are no hand-written request
  or response types — same shape as `financial-management`. Three kinds of hand-written value type
  are deliberate and stay: persisted enums in `entity/` (a contract rename must never turn into a
  data migration), a module's published API records in its base package (they are inter-module
  contracts, not wire types), and the forge port's records (they mirror GitHub, not SpecForge).
- Packages inside a module are **n-tier**, the same layout as the CarePay services
  (`financial-management`): `entity/`, `repository/`, `service/`, `api/`, `configuration/`,
  `exception/`, plus a port package where a module talks to something outside (`forge/`). There is
  no `internal/` segment — Spring Modulith already treats every sub-package of a module as internal
  and exposes only the base package, so the base package holds the module's published API and
  nothing else. `ModularityTests` fails on a reference into another module's sub-package, which is
  what makes the convention enforceable rather than aspirational.
- Spec versions are **content-addressed** — re-importing identical content is a no-op.
- Comment anchors are heading-slug-plus-ordinal and are **carried, never fuzzily reattached**;
  they go stale/orphaned instead.
- Diff is computed server-side and cached by (base hash, head hash), so reviewer, agent and audit
  all cite one artefact.
- **One review per spec**, not per pull request.
- Verdicts are version-scoped: a new head resets approvals.
- The approval gate has three inputs and **no override**.
- Accepting an agent suggestion creates a thread; it never edits the spec (follows from read-only).
- Default repository sync is **on pull request** — a PR touching a spec opens a review, and
  approvals report back as a GitHub status check. GitHub is the primary path; GitLab and Bitbucket
  are acknowledged only.

## Design canvas

The clickable UI prototype is published as an Artifact:
`https://claude.ai/code/artifact/3c85c291-80b5-4f80-999b-1fbf2cfe81d3`. Its sources (`design/`,
absent locally) were fragment files assembled by `design/build.mjs`; `design/specforge.html` is
**generated** — never edit it. Rebuild + serve with `npm --prefix design run dev`, or
`node design/serve.mjs [port]` (default 4173) and browse over http, not `file://`. On republish,
pin runtime `contract: "0.1.31"` and omit `capabilities`, or the stored grant is replaced.

The whole app is **one 1440×900 artboard** because artboards share no runtime state — a click can
never move between them, so any clickable multi-screen flow must live in a single `.dc.html`.
Violet is reserved for agent output (dashed square avatar, violet rail) so it is never mistaken
for a human's.

## Session conventions

- Working state lives in `hot.md` (## Work Log, ## Next); `hot.prev.md` is the previous session's
  rotation. Append a timestamped entry after a significant step, decision or blocker.
- Durable knowledge goes to the Obsidian brain at `~/.claude/memory/`, not into this repo — this
  project is `Projects/SpecForge/service-specforge.md`, linked to `topic-openspec`.
