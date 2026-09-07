## 1. Approval rules

- [ ] 1.1 Per-project rule (`min_approvals`, `require_roles`) persisted from the connect wizard's third step.
- [ ] 1.2 Rule evaluator returning satisfied plus a human-readable reason for both outcomes.
- [ ] 1.3 Rule editing by an administrator, audited, taking effect on open reviews immediately.

## 2. Required reviewers

- [ ] 2.1 `review_reviewer` rows: identity or role requirement, state (pending, approved, changes requested).
- [ ] 2.2 Populate required reviewers when a review opens, from the project configuration.
- [ ] 2.3 Add or remove a required reviewer on an open review (administrator only, audited).
- [ ] 2.4 Optional reviewers: anyone may cast a verdict; only required reviewers count toward the rule.

## 3. Verdicts

- [ ] 3.1 Cast approve, request changes or comment against the review's current head version, with an
      optional markdown body posted as a review-level comment.
- [ ] 3.2 Refuse approve and request changes from an agent identity with a conflict problem document.
- [ ] 3.3 Refuse a verdict cast against a stale head version, telling the reviewer the head advanced.
- [ ] 3.4 On head advance: reset approvals to pending with the reason recorded, keeping prior verdicts in history.

## 4. The approval gate

- [ ] 4.1 Gate evaluation combining rule state, unresolved blocking threads and failed blocking checks,
      each with its own reason line.
- [ ] 4.2 Transition to Approved only when the gate passes; refuse otherwise with the failing reasons.
- [ ] 4.3 Request changes transitions the specification to Changes Requested; a new head returns it to In Review.
- [ ] 4.4 Drive the pull request commit status from the outcome (pending, success, failure).

## 5. Review panel

- [ ] 5.1 Panel: status, required reviewers with their states, approvals x of y, unresolved conversations,
      check summary, rule state with its reason.
- [ ] 5.2 Composer with Approve / Request changes / Comment, disabled with an explanation when the actor
      may not cast that verdict.
- [ ] 5.3 Panel copy for each blocked reason, matching the prototype.

## 6. Verification

- [ ] 6.1 Tests: rule "2 approvals, at least one architect" is unsatisfied at 2 approvals from
      non-architects, and satisfied at 1 architect plus 1 reviewer.
- [ ] 6.2 Tests: agent approve is refused; approval with an unresolved blocking thread is refused;
      head advance resets approvals and the prior approval remains in history.
