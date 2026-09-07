## 1. Approval rules

- [x] 1.1 Per-project rule (`min_approvals`, `require_roles`) persisted from the connect wizard's third step.
      Already shipped with `add-spec-repository` (`ProjectEntity`, `ApprovalRule.yaml`); confirmed
      itest-covered by `SpecRepositoryTest.storesTheProjectConfigurationCapturedInTheWizard`.
- [x] 1.2 Rule evaluator returning satisfied plus a human-readable reason for both outcomes.
      `ApprovalGate.rule()`.
- [x] 1.3 Rule editing by an administrator, audited, taking effect on open reviews immediately.
      `PUT /api/projects/{projectName}/approval-rule`; "immediately" is `ReviewerReconciler` running
      on every read/verdict rather than a one-time snapshot at review-open time — see design.md note
      added below.

## 2. Required reviewers

- [x] 2.1 `review_reviewer` rows: identity or role requirement, state (pending, approved, changes requested).
- [x] 2.2 Populate required reviewers when a review opens, from the project configuration.
      Same reconcile path as 1.3, run lazily on first read rather than at open time — no separate
      "on open" hook needed.
- [x] 2.3 Add or remove a required reviewer on an open review (administrator only, audited).
- [x] 2.4 Optional reviewers: anyone may cast a verdict; only required reviewers count toward the rule.

## 3. Verdicts

- [x] 3.1 Cast approve, request changes or comment against the review's current head version, with an
      optional markdown body posted as a review-level comment.
      No separate "review-level comment" thread — the verdict's own `body` is the comment; a
      discussion thread is a different anchored concept and out of scope here.
- [x] 3.2 Refuse approve and request changes from an agent identity with a conflict problem document.
- [x] 3.3 Refuse a verdict cast against a stale head version, telling the reviewer the head advanced.
- [x] 3.4 On head advance: reset approvals to pending with the reason recorded, keeping prior verdicts in history.
      "Reason recorded" is structural, not a stored sentence: a reset seat's prior state is only ever
      visible through the verdict log, which is head-scoped, so the seat going back to pending needs
      no separate reason string to explain it.

## 4. The approval gate

- [x] 4.1 Gate evaluation combining rule state, unresolved blocking threads and failed blocking checks,
      each with its own reason line.
- [x] 4.2 Transition to Approved only when the gate passes; refuse otherwise with the failing reasons.
- [x] 4.3 Request changes transitions the specification to Changes Requested; a new head returns it to In Review.
      The In Review return was already implemented by `add-spec-review`'s `ReviewService.proposed()`,
      which calls `catalog.proposeChange()` unconditionally on every pushed head.
- [x] 4.4 Drive the pull request commit status from the outcome (pending, success, failure).

## 5. Review panel

- [x] 5.1 Panel: status, required reviewers with their states, approvals x of y, unresolved conversations,
      check summary, rule state with its reason. `ApprovalPanel.tsx`, a fourth "Approval" tab on the
      review screen, fed by `GET /api/reviews/{reviewId}/approval`.
- [x] 5.2 Composer with Approve / Request changes / Comment, disabled with an explanation when the actor
      may not cast that verdict.
- [x] 5.3 Panel copy for each blocked reason, matching the prototype. Read the actual prototype off the
      published design canvas artifact (`.rp-block`/`.rv`/`.gate` blocks, copy like "recorded in
      history") and reused its classes/wording; adapted the layout from the prototype's persistent
      sidebar to a fourth tab, matching how Discussions was already adapted the same way.

## 6. Verification

- [x] 6.1 Tests: rule "2 approvals, at least one architect" is unsatisfied at 2 approvals from
      non-architects, and satisfied at 1 architect plus 1 reviewer.
      `SpecApprovalTest.theRuleIsUnsatisfiedUntilAnArchitectApprovesAndSatisfiedOnceOneDoes`.
- [x] 6.2 Tests: agent approve is refused; approval with an unresolved blocking thread is refused;
      head advance resets approvals and the prior approval remains in history.
      `SpecApprovalTest.anAgentIdentityMayNeverApprove`,
      `.anUnresolvedBlockingThreadPreventsTheGateEvenWhenTheRuleIsSatisfied`,
      `.aHeadAdvanceResetsApprovalsToPendingAndOptionalVerdictsAreRecordedButDoNotCount`.
