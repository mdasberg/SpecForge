## ADDED Requirements

### Requirement: Append-only event log
The system SHALL record every state change as an immutable event carrying the acting identity, its
actor kind, the event kind, the subject, the specification and version context, the review context
where applicable, and a timestamp; and SHALL provide no path by which a recorded event can be modified
or deleted.

#### Scenario: State changes are recorded
- **WHEN** a specification version is imported, a review opened, a comment posted, a thread resolved,
  a verdict cast, a check completed, a ticket linked, or a lifecycle status transitioned
- **THEN** an event is recorded for each, naming the actor, the actor kind and the moment it happened

#### Scenario: Events are immutable
- **WHEN** any request or code path attempts to update or delete a recorded event
- **THEN** the attempt fails and the stored events are unchanged

#### Scenario: Agent actions are attributed to the agent
- **WHEN** an agent posts a finding and a human dismisses it
- **THEN** two events are recorded, the first attributed to the agent with actor kind agent and the
  second to the human

### Requirement: Specification timeline
The system SHALL present a specification's events as a chronological timeline, filterable by event kind
and by actor, each entry carrying a human-readable summary, so that what changed, why, who reviewed and
who approved are answerable without leaving the timeline.

#### Scenario: Timeline answers the review questions
- **WHEN** a user opens the history of a specification that was proposed, discussed, changed and approved
- **THEN** the timeline shows the versions and what changed in each, the discussions and their outcomes,
  each verdict with its author and version, and the approval with the rule that was satisfied

#### Scenario: Filter to human actions
- **WHEN** a user filters the timeline to humans only
- **THEN** agent-authored events are excluded and the remaining order is unchanged

#### Scenario: Timeline is paginated
- **WHEN** a specification has more events than one page
- **THEN** the timeline pages through them newest first without duplicating or skipping events

### Requirement: Version comparison from history
The system SHALL let a user select two version-created entries in the timeline and open the diff
between those versions.

#### Scenario: Comparing two historical versions
- **WHEN** a user selects the version 1 and version 3 entries in the timeline and compares them
- **THEN** the diff between version 1 and version 3 is rendered, in the same presentation as a review diff

### Requirement: Audit export
The system SHALL export a specification's complete audit record — versions, reviews, verdicts,
discussions, check results, linked tickets and status transitions — as JSON and as markdown, and SHALL
record the export itself as an event.

#### Scenario: Export is complete
- **WHEN** a user exports the audit record of an approved specification
- **THEN** the export contains every version, review, verdict, discussion, check result, linked ticket
  and status transition for that specification

#### Scenario: Export is audited
- **WHEN** an export is produced
- **THEN** an event records who exported which specification and when
