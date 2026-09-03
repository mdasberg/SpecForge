## ADDED Requirements

### Requirement: Threads anchored to a section or a text selection
The system SHALL let a reviewer open a comment thread anchored to a specification version and a
section, optionally narrowed to a selected text range within that section, from either the rendered
document or the diff, and SHALL store the quoted anchored text with the thread.

#### Scenario: Comment on a section
- **WHEN** a reviewer opens a thread on the Error Handling section
- **THEN** the thread is anchored to that section of that version and appears both on the document at
  that section and in the review's discussion list

#### Scenario: Comment on selected text
- **WHEN** a reviewer selects a phrase inside the API Contract section and comments on it
- **THEN** the thread is anchored to that text range, and the selected phrase is stored as its quoted
  original

#### Scenario: Comment on a changed line
- **WHEN** a reviewer comments on a changed line in the diff
- **THEN** the thread is anchored to the section containing that line and shows in both diff modes

### Requirement: Replies and mentions
The system SHALL support ordered replies within a thread and SHALL notify a project member who is
mentioned by handle in a comment, as well as participants in a thread that receives a reply.

#### Scenario: Reply order is preserved
- **WHEN** three replies are posted to a thread
- **THEN** they render in the order they were posted, each with author and timestamp

#### Scenario: Mention notifies
- **WHEN** a comment mentions a project member by handle
- **THEN** that member receives an in-app notification linking to the thread

#### Scenario: Mention of a non-member does not notify
- **WHEN** a comment mentions a handle that is not a member of the project
- **THEN** no notification is created and the text renders without a link

### Requirement: Resolve and reopen
The system SHALL let a thread be resolved and reopened, SHALL record the actor and timestamp of each
resolution and reopening, and SHALL expose the count of unresolved threads per review for approval
gating.

#### Scenario: Resolving records who and when
- **WHEN** a reviewer resolves a thread
- **THEN** the thread is marked resolved with that reviewer and the current timestamp, and its
  comments remain readable

#### Scenario: Resolution is consistent across surfaces
- **WHEN** a thread is resolved from the rendered document
- **THEN** the review's unresolved count and the discussion list reflect it immediately

#### Scenario: Reopening restores the count
- **WHEN** a resolved thread is reopened
- **THEN** it counts as unresolved again and the reopening is recorded

### Requirement: Threads are anchored to the version they were written against
The system SHALL show, for every thread, the specification version it was written against, and SHALL
render a thread whose anchored text has since changed as outdated, presenting the quoted original and
a link to the version in which it applied, rather than reattaching it to different text.

#### Scenario: Thread written against an earlier version
- **WHEN** a thread written against version 2 is viewed in a review at version 4
- **THEN** the thread states that it was written against version 2

#### Scenario: Outdated thread keeps its original quote
- **WHEN** the text a thread anchors to is rewritten in a later version
- **THEN** the thread renders as outdated with the original quoted text and does not appear attached to
  the new text

### Requirement: Author provenance on every comment
The system SHALL record on every comment whether its author is a human or an agent, SHALL expose that
on every read of the comment, and the interface SHALL distinguish agent authorship visually and not
by name alone.

#### Scenario: Agent comment is marked in the API
- **WHEN** any comment written by an agent identity is read through the API
- **THEN** the response carries an actor kind of agent for that comment

#### Scenario: Agent comment is visually distinct
- **WHEN** a thread contains both a human comment and an agent comment
- **THEN** the agent comment is rendered with the agent treatment defined by the design system, so its
  origin is obvious without reading the author name

#### Scenario: Provenance cannot be omitted
- **WHEN** a comment is returned by any endpoint
- **THEN** the actor kind field is present
