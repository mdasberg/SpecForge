## ADDED Requirements

### Requirement: Required reviewers with per-reviewer state
The system SHALL hold for each review a set of required reviewers, named individually or by role,
each in a state of pending, approved or changes requested, and SHALL let anyone who may read the
review also cast a verdict, counting only required reviewers toward the approval rule.

#### Scenario: Required reviewers start pending
- **WHEN** a review opens on a project requiring three reviewers
- **THEN** all three are listed as pending and the review reports zero of the required approvals

#### Scenario: Optional verdicts are recorded but do not count
- **WHEN** a reviewer who is not required approves the review
- **THEN** the approval is recorded and visible, and the required approval count is unchanged

#### Scenario: Required reviewers can be changed by an administrator
- **WHEN** an administrator removes a required reviewer who has left the team
- **THEN** the review's required set and rule evaluation update, and the change is recorded in history

### Requirement: Verdicts are cast against a version
The system SHALL record every verdict — approve, request changes or comment — with its author, an
optional body, a timestamp and the head version it was cast against, and SHALL refuse a verdict cast
against a version that is no longer the review's head.

#### Scenario: Approval names its version
- **WHEN** a reviewer approves a review whose head is version 3
- **THEN** the approval is recorded against version 3

#### Scenario: Stale verdict is refused
- **WHEN** a reviewer submits an approval for version 3 after the head advanced to version 4
- **THEN** the verdict is refused with a message stating that the head advanced, and no approval is recorded

#### Scenario: Comment verdict does not approve
- **WHEN** a required reviewer submits a comment verdict
- **THEN** their state stays pending and the approval count is unchanged

### Requirement: Project approval rules
The system SHALL evaluate a per-project approval rule consisting of a minimum number of approvals and
an optional set of required roles, and SHALL report the outcome together with a human-readable reason
whether or not it is satisfied.

#### Scenario: Rule requiring an architect is unsatisfied without one
- **WHEN** a rule of two approvals including at least one architect has two approvals from
  non-architect reviewers
- **THEN** the rule is unsatisfied and the reason states that an approval from an architect is still needed

#### Scenario: Rule satisfied
- **WHEN** that rule has one approval from an architect and one from another reviewer
- **THEN** the rule is satisfied and the reason states it is met

#### Scenario: Reason is always available
- **WHEN** the rule state is read for any open review
- **THEN** a reason sentence is present in both the satisfied and the unsatisfied case

### Requirement: Approval gate
The system SHALL permit a review to reach Approved only when its approval rule is satisfied, no
blocking discussion thread is unresolved, and no blocking automated check has failed, and SHALL refuse
the transition otherwise, naming every failing condition. There SHALL be no override that approves a
review past this gate.

#### Scenario: Unresolved blocking conversation prevents approval
- **WHEN** the approval rule is satisfied but two blocking threads are unresolved
- **THEN** the review does not reach Approved and the refusal names the unresolved conversations

#### Scenario: Failed blocking check prevents approval
- **WHEN** the rule is satisfied, all threads are resolved, and one blocking check has failed
- **THEN** the review does not reach Approved and the refusal names the failed check

#### Scenario: Gate passes
- **WHEN** the rule is satisfied, no blocking thread is unresolved and no blocking check has failed
- **THEN** the review reaches Approved and the specification's status becomes Approved

#### Scenario: No override exists
- **WHEN** any actor, including an administrator, attempts to approve a review whose gate fails
- **THEN** the request is refused

### Requirement: A new head version invalidates approvals
The system SHALL, when a review's head version advances, return every approved reviewer to pending,
record the reason for the invalidation, and retain the earlier verdicts in the review's history.

#### Scenario: Approvals reset on a new version
- **WHEN** a review with two approvals receives a new head version
- **THEN** both reviewers return to pending, the reason records that the head advanced, and both
  earlier approvals remain visible in history

#### Scenario: An approved specification re-enters review
- **WHEN** a new change is proposed for an approved specification
- **THEN** the specification returns to In Review and the previous approval remains recorded against
  the version it was cast on

### Requirement: Agents never approve
The system SHALL refuse an approve or request-changes verdict from an agent identity, and SHALL allow
agents to contribute comments and check results only.

#### Scenario: Agent approval refused
- **WHEN** an agent identity submits an approve verdict
- **THEN** the request is refused with a conflict problem document stating that agents cannot approve,
  and no verdict is recorded

#### Scenario: Agent comment allowed
- **WHEN** an agent identity submits a comment
- **THEN** it is recorded as an agent-authored comment and the approval count is unaffected

### Requirement: Verdicts drive the lifecycle and the reported status
The system SHALL move the specification to Changes Requested when a required reviewer requests
changes, back to In Review when a new head version arrives, and to Approved when the gate passes, and
SHALL report the current review outcome to the originating pull request.

#### Scenario: Request changes moves the specification
- **WHEN** a required reviewer requests changes
- **THEN** the specification's status becomes Changes Requested and the pull request status becomes failed

#### Scenario: Approval reports success
- **WHEN** the review reaches Approved
- **THEN** the pull request status becomes successful
