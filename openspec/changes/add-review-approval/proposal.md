## Why

A review that cannot conclude is a discussion forum. The point of SpecForge is that a specification
reaches a defensible Approved state: the required people looked at it, the project's rule was
satisfied ("two approvals, at least one from an architect"), nothing blocking was left unresolved,
and the decision is recorded against a specific version. This change delivers the review panel and
the verdict machinery behind it — and the rule that agents never approve.

## What Changes

- Required reviewers per review, named individually or by role, each with a state of pending,
  approved or changes requested.
- Verdicts: approve, request changes, or comment, each with an optional body, recorded against the
  head version they were cast on.
- Per-project approval rules: a minimum number of approvals plus optional role constraints,
  evaluated to satisfied or unsatisfied with a human-readable reason.
- An approval gate: Approved requires the rule satisfied, every blocking thread resolved, and every
  blocking check passed.
- A new head version invalidates prior approvals, returning those reviewers to pending and recording
  why.
- Agents may comment; approve and request-changes from an agent identity are refused.
- Lifecycle transitions driven by verdicts, and the pull request status updated from the outcome.

## Capabilities

### New Capabilities
- `review-approval`: required reviewers, verdicts, approval rules, the approval gate and the
  lifecycle transitions they drive.

### Modified Capabilities
- `spec-repository`: none in requirements — the status-report hook added there is now driven by
  approval outcomes.

## Impact

- New module `approval`; tables for review reviewers, verdicts and per-project approval rules.
- Reads the unresolved blocking thread count from `discussions` and the blocking check state from
  `automated-review` through read ports; the gate degrades to "checks not configured" until
  `add-automated-review` ships.
- Frontend: the review panel (status, reviewers, counts, rule state, checks) and the
  approve / request changes / comment composer.
- Depends on: `add-review-discussions`. Fully realised once `add-automated-review` ships.
