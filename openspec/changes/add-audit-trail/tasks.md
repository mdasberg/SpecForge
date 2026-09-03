## 1. Event log

- [ ] 1.1 `audit_event` table (id, occurred at, actor, actor kind, kind, subject type, subject id,
      specification, version, review, payload) with insert-only access enforced at the repository and
      by database grants.
- [ ] 1.2 Event kinds covering import, version created, review opened, head advanced, comment posted,
      thread resolved and reopened, verdict cast, approvals invalidated, rule evaluated, check run
      completed, finding disposed, ticket linked and unlinked, status transitioned, configuration changed.
- [ ] 1.3 Consume each module's Modulith events and write them; no module writes the log directly.
- [ ] 1.4 Test enumerating state-changing endpoints and asserting each produces an event.

## 2. Timeline

- [ ] 2.1 Per-specification timeline query, newest first, paginated, with actor and version context.
- [ ] 2.2 Filters by event kind and actor, including a humans-only and an agents-only view.
- [ ] 2.3 Event rendering with a one-line human-readable summary per kind, matching the prototype.
- [ ] 2.4 Launch a version comparison from any two version-created events.

## 3. Export

- [ ] 3.1 Audit export for one specification as JSON: versions, reviews, verdicts, threads, checks,
      tickets and transitions.
- [ ] 3.2 The same export as markdown for attaching to a compliance record.
- [ ] 3.3 Export is itself an audited event, recording who exported what and when.

## 4. Traceability chain

- [ ] 4.1 Chain read model per approved specification version: approval, tickets, pull requests, deployments.
- [ ] 4.2 Discover pull requests from the review's originating pull request and from linked tickets'
      referenced branches.
- [ ] 4.3 Ingest deployment events from GitHub deployments through the existing webhook; generic ingest
      endpoint for other pipelines.
- [ ] 4.4 Mark the specification Implemented when a pull request implementing it deploys to production.
- [ ] 4.5 Gap detection: approved without a ticket, ticket without a pull request, merged without a
      deployment — each rendered as a named gap.

## 5. Screens

- [ ] 5.1 History tab: timeline with filters and the compare action.
- [ ] 5.2 Traceability tab: the chain with its gaps.
- [ ] 5.3 Activity screen reading the event log across projects, filterable by project, actor and kind.

## 6. Verification

- [ ] 6.1 Tests: an event cannot be updated or deleted through any code path.
- [ ] 6.2 Test: the full designed flow — proposal, comments, ticket, changes requested, new version,
      approval, merge, deployment — produces a timeline that answers what changed, why, who reviewed
      and who approved.
