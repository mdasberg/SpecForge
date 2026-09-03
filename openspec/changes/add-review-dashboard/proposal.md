## Why

By this point every capability works, but a reviewer arriving in the morning still has to go looking
for their work. The dashboard is the one screen that makes the product a habit: what needs my review,
what am I waiting on, what did I ask for changes on, what was recently approved or implemented. Each
row has to carry enough to decide whether to open it — version, approvals, unresolved conversations,
blocking check state, age.

It ships last because every queue is a projection of a capability that has to exist first.

## What Changes

- Home queues: needs your review, awaiting approval, changes requested, recently approved, recently
  implemented, recently changed — each capped, each with a link to the equivalent filtered catalog view.
- Queue rows carrying specification, project and domain, version, approvals against the rule,
  unresolved conversation count, blocking check state, and age.
- Activity feed across projects, filterable by project, actor and event kind, reading the audit log.
- Empty states per queue, and a stale marker on reviews untouched beyond a threshold.

## Capabilities

### New Capabilities
- `review-dashboard`: the personal review queues, their row content, and the cross-project activity feed.

### Modified Capabilities
(none — the dashboard is a read model over existing capabilities)

## Impact

- New module `dashboard`, read-only, querying reviews, approvals, discussions, checks and the audit log
  through their read ports. No new tables beyond a materialised queue projection if the queries prove slow.
- Frontend: the Home screen and the Activity screen.
- Depends on: `add-audit-trail` (and through it everything before).
