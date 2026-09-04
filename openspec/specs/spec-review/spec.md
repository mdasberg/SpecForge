# spec-review

## Purpose

The screen the product exists for. A reviewer opens a changed specification and has to answer one
question quickly: what actually changed, and does it hold up? A review is one specification between
two versions — a base and a head — with every section classified, a line diff inside the ones that
changed, and both an inline and a side-by-side rendering of the same computed artefact. It is also
where the addressing scheme the rest of the product hangs off is settled: anchors are carried across
a head update, never fuzzily reattached, so a comment written against version 2 still means
something at version 4.

## Requirements

### Requirement: A review targets one specification between two versions
The system SHALL represent a review as a review of one specification, holding a base version and a
head version, an open or closed state, and the pull request reference when the review originated
from one; and SHALL open one review per changed specification when a change is proposed, updating
the existing review's head rather than opening a second review for the same proposal.

#### Scenario: Proposed change opens a review
- **WHEN** a change is proposed for a specification currently at version 3
- **THEN** a review is opened with base version 3 and the proposed content as its head, and the
  specification's status becomes In Review

#### Scenario: A pull request touching four specifications opens four reviews
- **WHEN** a pull request modifies four matched specification files
- **THEN** four reviews are opened, one per specification, each referencing that pull request

#### Scenario: A further commit updates the head
- **WHEN** another commit is pushed to a pull request that already has an open review
- **THEN** that review's head is replaced and no second review is created

### Requirement: Section-level diff between versions
The system SHALL compute, for a review's base and head, a diff that classifies every section as
added, removed, modified or unchanged, provides a line-level diff with word-level change ranges
within each modified section, and reports summary counts of added, removed and modified sections.

#### Scenario: Body edit is one modified section
- **WHEN** the head changes two sentences inside the Validation Rules section only
- **THEN** the diff reports one modified section, with the changed lines and the changed words within
  them identified, and every other section unchanged

#### Scenario: New section is added, not modified
- **WHEN** the head introduces a Domain Events section that the base did not have
- **THEN** the diff reports it as added

#### Scenario: Renamed heading is a removal plus an addition
- **WHEN** the head renames a section heading while keeping its body
- **THEN** the diff reports the old section removed and the new section added

#### Scenario: Diff is stable for the same content
- **WHEN** the same base and head content are diffed again in another review
- **THEN** the identical diff is returned from cache without recomputation

### Requirement: Inline and side-by-side rendering
The system SHALL render a review's diff both inline and side-by-side as two presentations of the same
computed diff, SHALL let the reviewer switch between them without losing position, and SHALL remember
the reviewer's preferred mode.

#### Scenario: Switching mode keeps the change set
- **WHEN** a reviewer switches from inline to side-by-side
- **THEN** the same added, removed and modified content is shown, arranged as previous and current,
  with no refetch

#### Scenario: Preference is remembered
- **WHEN** a reviewer who last used side-by-side opens another review
- **THEN** the diff opens in side-by-side

### Requirement: Changed-section navigation and attribution
The system SHALL list a review's changed sections with the number of changes in each, SHALL scroll to
a section when it is selected, and SHALL show for each changed section who changed it and when.

#### Scenario: Jump list reflects the diff
- **WHEN** a review has three changed sections
- **THEN** the jump list names those three with their change counts and nothing else

#### Scenario: Attribution per changed section
- **WHEN** a reviewer inspects a changed section
- **THEN** the author and timestamp of the commit that changed it are shown

### Requirement: Anchors survive a head update
The system SHALL, when a review's head advances, keep anchors attached to sections whose key and
content are unchanged, mark anchors on sections whose content changed as stale, and mark anchors whose
section key no longer exists as orphaned while retaining the originally quoted text.

#### Scenario: Untouched section keeps its anchors
- **WHEN** a review's head advances with a change confined to the API Contract section
- **THEN** anchors on every other section remain current

#### Scenario: Edited section makes its anchors stale
- **WHEN** the head changes the text an anchor addresses
- **THEN** that anchor is marked stale, still attached to the section, and the originally quoted text
  is retained

#### Scenario: Removed section orphans its anchors
- **WHEN** the head removes the section an anchor addresses
- **THEN** that anchor is marked orphaned and renders against the quoted original rather than
  attaching to another section

### Requirement: Compare arbitrary versions without opening a review
The system SHALL let a reader compare any two versions of a specification and see the same diff
presentation, without creating a review, reviewers or review history.

#### Scenario: Ad-hoc comparison
- **WHEN** a reader compares version 1 with version 3 of a specification
- **THEN** the diff is rendered, and no review is created and no review event recorded
