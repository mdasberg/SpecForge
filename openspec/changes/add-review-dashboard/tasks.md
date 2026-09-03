## 1. Queues

- [ ] 1.1 Needs your review: open reviews where the current user is a required reviewer and pending.
- [ ] 1.2 Awaiting approval: open reviews the user has approved or opened, where the rule is not yet satisfied.
- [ ] 1.3 Changes requested: reviews where the user requested changes, or which they own and are at
      Changes Requested.
- [ ] 1.4 Recently approved and recently implemented, scoped to the user's projects, over a rolling window.
- [ ] 1.5 Recently changed: specifications in the user's projects with a new version in the window.
- [ ] 1.6 Cap each queue and link to the filtered catalog view showing the rest.

## 2. Row content

- [ ] 2.1 Row projection: title, project, domain, status, head version, approvals against the rule,
      unresolved conversation count, blocking check state, age since last activity.
- [ ] 2.2 One query per queue, no per-row lookups; assert the query count in a test.
- [ ] 2.3 Stale marker on open reviews with no activity beyond the configured threshold.

## 3. Activity feed

- [ ] 3.1 Cross-project feed over the audit log, newest first, paginated.
- [ ] 3.2 Filters by project, actor and event kind, including humans-only and agents-only.
- [ ] 3.3 Each entry links to the subject it describes.

## 4. Screens

- [ ] 4.1 Home screen with the six queues, per the prototype's layout and density.
- [ ] 4.2 Per-queue empty states that say what would put something there.
- [ ] 4.3 Activity screen with its filters.

## 5. Verification

- [ ] 5.1 Tests: a reviewer sees only their own queues; approving removes the review from needs-your-review
      and adds it to awaiting approval; a new head version returns it to needs-your-review.
- [ ] 5.2 Test: row counts match the review panel's counts for the same review.
