## ADDED Requirements

### Requirement: Check runs per review head
The system SHALL run a project's configured set of checks against a review's head version, SHALL
record each run's state as queued, running, passed, failed or skipped together with its duration and
summary, and SHALL mark every check as either blocking or advisory as configured for the project.

#### Scenario: Checks run when a review opens
- **WHEN** a review opens with eight checks enabled for its project
- **THEN** eight check runs are created for the head version and progress from queued through running
  to a terminal state

#### Scenario: A check reports failure with a summary
- **WHEN** the acceptance-criteria check finds a requirement without scenarios
- **THEN** its run is failed, with a summary naming the requirement, and it is marked blocking

#### Scenario: A disabled check is not run
- **WHEN** a project has the terminology check disabled
- **THEN** no run is created for it and the review does not report it as skipped work

#### Scenario: Checks can be rerun
- **WHEN** a reviewer reruns a failed check on the current head
- **THEN** a new run is created, the previous run is marked stale, and the previous run remains inspectable

### Requirement: Checks re-run when the head advances
The system SHALL dispatch a fresh set of check runs when a review's head version advances, SHALL mark
the runs against the superseded head as stale, and SHALL cancel runs still queued for the superseded head.

#### Scenario: New version triggers new runs
- **WHEN** a review's head advances from version 3 to version 4
- **THEN** the version 3 runs are marked stale and a new set of runs is dispatched for version 4

#### Scenario: Queued runs for an old head are cancelled
- **WHEN** the head advances while runs for the previous head are still queued
- **THEN** those queued runs are cancelled rather than executed

### Requirement: Findings are agent-authored and anchored
The system SHALL record each finding with the check run that produced it, the agent identity that
authored it, the run reference including the model identity where a model produced it, a severity, a
title, a body, an optional proposed text, and an anchor to a section and optional text range; and SHALL
present findings through the same anchored presentation as human comments while rendering them as
agent output.

#### Scenario: Finding is anchored like a comment
- **WHEN** the API compatibility check reports a breaking change in the API Contract section
- **THEN** the finding appears anchored to that section in the document and the diff

#### Scenario: Finding names its agent and run
- **WHEN** a reviewer inspects a model-backed finding
- **THEN** the producing agent, the run and the model identity are shown

#### Scenario: Findings are never mistaken for human comments
- **WHEN** a section carries both a human thread and an agent finding
- **THEN** the finding is rendered with the agent treatment defined by the design system, distinct from
  the human thread

### Requirement: Findings can be accepted, dismissed or discussed
The system SHALL let a human accept, dismiss or discuss a finding; accepting SHALL record the
disposition and open an anchored thread carrying the finding's proposed text attributed to the
accepting human with the agent suggestion quoted; dismissing SHALL record the actor and timestamp and
remove the finding from the active list while retaining it in history; discussing SHALL open a thread
on the finding's anchor without recording a disposition. Accept and dismiss SHALL be reversible while
the review is open.

#### Scenario: Accepting a suggestion
- **WHEN** a reviewer accepts a finding that proposes an added validation rule
- **THEN** the disposition is recorded and an anchored thread is created carrying the proposed text,
  attributed to that reviewer and quoting the agent's suggestion

#### Scenario: Accepting does not change the specification
- **WHEN** a suggestion is accepted
- **THEN** no specification content is modified, in SpecForge or in the repository

#### Scenario: Dismissing is recorded and reversible
- **WHEN** a reviewer dismisses a finding and then undoes the dismissal while the review is open
- **THEN** both actions are recorded and the finding is active again

#### Scenario: Discussing leaves the finding open
- **WHEN** a reviewer opens a discussion on a finding
- **THEN** a thread is created on the finding's anchor and the finding remains undisposed

### Requirement: Only blocking checks affect approval
The system SHALL expose failed blocking checks to the approval gate and SHALL ensure advisory check
results never prevent an approval.

#### Scenario: Advisory failure does not block
- **WHEN** the terminology check fails as advisory and no blocking check has failed
- **THEN** the approval gate's check condition is satisfied

#### Scenario: Blocking failure blocks
- **WHEN** the OpenSpec validation check fails as blocking
- **THEN** the approval gate's check condition is unsatisfied and names that check

#### Scenario: A check's blocking status is a project decision
- **WHEN** an administrator changes a check from advisory to blocking
- **THEN** the change is recorded in history and the gate applies it to open reviews

### Requirement: Agents cannot act with human authority
The system SHALL refuse, for any request authenticated as an agent identity, the casting of approve or
request-changes verdicts and the resolution or reopening of human discussion threads.

#### Scenario: Agent cannot resolve a human thread
- **WHEN** a request authenticated as an agent attempts to resolve a human thread
- **THEN** the request is refused and the thread remains unresolved

#### Scenario: Agent contribution stays within comments and checks
- **WHEN** an agent identity posts a comment and a finding
- **THEN** both are accepted and recorded as agent-authored
