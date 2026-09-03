## Why

Six months after a specification was approved, someone asks: what changed, why, who reviewed it, who
approved it, and did it ever ship? Every capability so far produces the raw material for those
answers, and none of them can answer alone. This change makes the record first-class — an
append-only event log written by every capability, a timeline that reads it, and the
specification-to-deployment chain that turns "approved" into "approved, ticketed, merged, deployed".

It is also the compliance surface: a spec's history has to be exportable without an engineer
querying a database.

## What Changes

- Append-only event log: every state change recorded with actor (human or agent), timestamp, subject
  and context; events immutable once written.
- Per-specification timeline, chronological, filterable by event kind and actor, answering what
  changed, why, who reviewed and who approved.
- Version comparison launched from the timeline, reusing the review diff.
- Audit export for a specification, as JSON and as markdown.
- Traceability chain per approved specification version: approval, implementation ticket, pull
  request, deployment — each link with its source and timestamp.
- Gaps in the chain shown explicitly: approved with no ticket, ticketed with no pull request, merged
  with no deployment.

## Capabilities

### New Capabilities
- `audit-trail`: the append-only event log, the per-specification timeline, version comparison from
  history and the audit export.
- `traceability`: the specification version to approval to ticket to pull request to deployment
  chain, including explicit gaps.

### Modified Capabilities
(none — every capability already emits domain events; this change persists and presents them)

## Impact

- New module `audit`; one append-only `audit_event` table with no update or delete path, plus a
  `deployment` table and the chain read model.
- Every existing module gains event emission on state change; a test enumerates state-changing
  endpoints and asserts each writes an event.
- Deployment data has to come from somewhere: GitHub deployment events via the existing webhook, with
  a generic ingest endpoint for other pipelines.
- Frontend: the History and Traceability tabs, plus the Activity screen's data source.
- Depends on: `add-review-approval`, `add-ticket-links`. Deployment links need a pipeline that reports.
