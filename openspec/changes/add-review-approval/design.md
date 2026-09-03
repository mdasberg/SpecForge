## Context

Approval is the product's only irreversible-feeling act, and the one auditors ask about. It has to be
unambiguous about what was approved, by whom, under which rule, and whether that approval still
stands after the document moved.

## Goals / Non-Goals

**Goals:**
- An approval always names a version. "Approved" without a version is not a claim worth recording.
- The rule's verdict is explainable in one sentence to the person blocked by it.
- Human accountability is structurally guaranteed, not merely conventional.

**Non-Goals:**
- Arbitrary rule expressions or a policy DSL.
- Merging, or blocking a merge by any means other than the reported status.
- Delegation, quorum-by-team or approval expiry.

## Decisions

- **Verdicts are version-scoped, and a new head resets approvals.** An approval of version 3 says
  nothing about version 4. Reviewers return to pending with the reason recorded ("head advanced to
  v4"), and the prior approval stays in history. The alternative — carrying approvals across
  "non-substantive" changes — needs a machine to judge substance, which is exactly the judgment the
  approval exists to capture.
- **Rules are a fixed shape: minimum count plus role constraints.** `minApprovals: 2,
  requireRoles: [ARCHITECT]` covers the designed rule and everything adjacent. A DSL would be
  unjustifiable for two fields.
- **The rule evaluator returns a reason, always.** Satisfied or not, it produces the sentence the UI
  shows ("1 of 2 approvals; needs one from an architect"). One source for the state and its
  explanation, so panel and API cannot disagree.
- **Agents never approve — enforced at the verdict boundary.** An agent identity casting approve or
  request-changes is refused with a conflict, not filtered in the UI. Agents block through failing
  checks, which a human then judges.
- **The gate has three inputs and no override.** Rule satisfied, no unresolved blocking thread, no
  failed blocking check. An administrator can mark a check advisory or a thread non-blocking — a
  decision that is itself audited — but cannot force an approval past the gate.
- **Request changes moves the specification to Changes Requested immediately**, and a subsequent head
  version moves it back to In Review. The status follows the last verdict, not a manual toggle.

## Risks / Trade-offs

- Resetting approvals on every head update will irritate reviewers on typo-fix pushes. Accepted for
  honesty; the mitigation is social (batch the fixes), and re-approving a version whose diff is one
  word is a single click.
- No override means a stuck review with a departed required reviewer needs an administrator to change
  the required reviewers — an audited action. Deliberate: a break-glass approval is the one thing an
  audit trail cannot reconstruct.
- Role constraints depend on the coarse role model from `add-project-skeleton`. "The owning team's
  architect" is not expressible yet; it is a contained change inside the evaluator.
