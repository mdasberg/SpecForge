## ADDED Requirements

### Requirement: Personal review queues
The system SHALL present on the home screen six queues scoped to the signed-in user — specifications
needing their review, reviews awaiting approval, reviews with changes requested, recently approved,
recently implemented, and recently changed — each capped in length with a link to the filtered list
holding the remainder.

#### Scenario: Needs your review
- **WHEN** a user is a required reviewer on three open reviews and has cast no verdict on them
- **THEN** all three appear in needs your review

#### Scenario: Approving moves the row
- **WHEN** that user approves one of those reviews while the approval rule is not yet satisfied
- **THEN** the review leaves needs your review and appears in awaiting approval

#### Scenario: A new version returns the review
- **WHEN** the head version advances on a review the user had approved
- **THEN** the review appears in needs your review again

#### Scenario: Queues are personal
- **WHEN** two users with different required reviews open the home screen
- **THEN** each sees only the reviews that concern them

#### Scenario: Overflow is reachable
- **WHEN** a queue holds more reviews than it displays
- **THEN** the queue states how many more there are and links to the filtered list containing them

### Requirement: Queue rows carry the decision context
The system SHALL show for each queue row the specification title, its project and domain, its lifecycle
status, its head version, the approvals counted against the rule, the number of unresolved
conversations, the blocking check state, and the time since the last activity.

#### Scenario: A row shows what a reviewer needs
- **WHEN** a review at version 3 has two of three required approvals, four unresolved conversations and a
  failed blocking check
- **THEN** its row shows version 3, two of three approvals, four unresolved conversations and the failed
  blocking check

#### Scenario: Row and panel agree
- **WHEN** a user compares a queue row with the review panel of the same review
- **THEN** the approvals, unresolved conversations and check state are identical

#### Scenario: Stale reviews are marked
- **WHEN** an open review has had no activity beyond the configured staleness threshold
- **THEN** its row is marked stale with the time since last activity

### Requirement: Cross-project activity feed
The system SHALL present an activity feed of recent events across the projects the user may see,
newest first, filterable by project, actor and event kind, with each entry linking to its subject.

#### Scenario: Feed spans projects
- **WHEN** a user who is a member of two projects opens the activity feed
- **THEN** events from both projects appear, newest first

#### Scenario: Filter to agents
- **WHEN** the user filters the feed to agent activity
- **THEN** only agent-authored events are listed

#### Scenario: Entries link to their subject
- **WHEN** the user selects an entry describing a resolved conversation
- **THEN** that conversation opens on the specification it belongs to

### Requirement: Empty queues explain themselves
The system SHALL render an empty queue with a statement of what would place an item in it, rather than
as blank space.

#### Scenario: Nothing to review
- **WHEN** a user has no review to act on
- **THEN** the needs your review queue states that nothing is waiting on them and what would put
  something there
