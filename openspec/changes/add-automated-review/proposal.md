## Why

Most of what goes wrong in a specification is mechanically detectable: acceptance criteria missing,
an API contract that breaks its consumers, an error code that contradicts the platform's
conventions, terminology drifting, an edge case never mentioned. Having a human find those is a
waste of the only reviewer who can judge intent. Automated and AI reviewers should do the first
pass and participate in the review like any other reviewer — visibly, attributably, and without
authority to approve.

That last point is the design constraint: agent feedback that reads like a colleague's is a
liability. Provenance is already an API-level field from `add-review-discussions`; this change adds
the checks and findings, and the accept / dismiss / discuss handling.

## What Changes

- Check runs per review head: a named set of checks, each queued, running, passed, failed or
  skipped, each blocking or advisory, with duration and a rerun action.
- The initial check set: OpenSpec structural validation, acceptance-criteria coverage, API
  compatibility, architecture rules, security review, terminology consistency, missing edge cases,
  breaking changes.
- Findings: an agent-authored, anchored finding with a severity, produced by a named agent with its
  run reference, rendered as agent output everywhere.
- Suggestions can be accepted, dismissed or discussed; accepting turns the suggestion into an
  anchored thread carrying the proposed text; both accept and dismiss record who and when and are
  reversible while the review is open.
- Advisory checks never block approval; blocking checks do, through the gate from
  `add-review-approval`.
- Checks re-run automatically when the head advances, and prior results are marked stale.

## Capabilities

### New Capabilities
- `automated-review`: check runs, agent findings and suggestions, their provenance, their
  accept / dismiss / discuss handling, and their effect on approval.

### Modified Capabilities
- `review-approval`: none in requirements — the gate's blocking-check input is now populated.

## Impact

- New module `agent`; tables for check definitions, check runs, findings and finding dispositions.
- Checks execute out of process behind a `CheckRunner` port: structural and rule checks run locally,
  the model-backed reviews call an LLM. The port keeps the review flow indifferent to which is which.
- Agent identities are rows in the same identity table as humans, with actor kind `AGENT` — so an
  agent's output cannot be stored without provenance.
- Depends on: `add-review-approval`.
