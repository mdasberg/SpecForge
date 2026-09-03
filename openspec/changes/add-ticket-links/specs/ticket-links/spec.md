## ADDED Requirements

### Requirement: Create a ticket from a comment
The system SHALL let a user create a tracker ticket from a comment through a form prefilled from that
comment, capturing a title, a description, a target project, a priority and an assignee, and SHALL
record the resulting ticket as linked to that discussion thread.

#### Scenario: Ticket created from a discussion
- **WHEN** a reviewer creates a ticket from a comment asking for an added validation rule
- **THEN** the ticket is created in the project's configured tracker with the entered title,
  description, priority and assignee, and the thread shows the new ticket

#### Scenario: Description carries the review context
- **WHEN** a ticket is created from a comment anchored to the Error Handling section
- **THEN** the ticket description includes the comment body, the quoted specification text and a
  permalink back to the thread

#### Scenario: Creation failure preserves input
- **WHEN** the tracker rejects the creation
- **THEN** the entered values are retained, the tracker's message is shown, and no link is recorded

#### Scenario: Tracker choices come from the tracker
- **WHEN** the form is opened for a project using Jira
- **THEN** the selectable projects, priorities and assignees are those the Jira configuration exposes

### Requirement: Trackers are interchangeable
The system SHALL support GitHub Issues, Jira and Linear as equal implementations of one tracker port,
SHALL let a project configure exactly one of them, and SHALL keep the ticket-creation flow identical
across them.

#### Scenario: The flow does not change with the tracker
- **WHEN** two projects using different trackers each create a ticket from a comment
- **THEN** both use the same form and the same five fields, and each ticket lands in that project's tracker

#### Scenario: No tracker configured
- **WHEN** a project has no tracker configured
- **THEN** the create-ticket action is shown disabled with the reason that no tracker is configured

#### Scenario: Switching a project's tracker keeps existing links
- **WHEN** an administrator changes a project's tracker
- **THEN** previously created links keep pointing at their original tracker and remain readable

### Requirement: Two-way linkage between discussion and ticket
The system SHALL write a permalink to the originating discussion into the created ticket, and SHALL
show on the discussion the linked ticket's key, title, status and assignee, refreshed from the tracker,
labelled with how recently it was refreshed.

#### Scenario: Backlink is in the ticket
- **WHEN** a ticket created from SpecForge is opened in the tracker
- **THEN** its description contains a link that resolves to the originating discussion thread

#### Scenario: Status is refreshed
- **WHEN** a linked ticket moves to In Progress in the tracker
- **THEN** the discussion shows the ticket as In Progress, with the time of the last refresh

#### Scenario: Unavailable ticket stays visible as a link
- **WHEN** a linked ticket is deleted in the tracker
- **THEN** the link remains on the discussion marked unavailable, and the fact that a ticket was created
  is not lost

### Requirement: Link and unlink existing tickets
The system SHALL let a user link an existing ticket to a discussion by key or URL after verifying it
exists, and SHALL let a link be removed while retaining the linking and unlinking in history.

#### Scenario: Existing ticket linked
- **WHEN** a reviewer links an existing ticket by its key
- **THEN** the ticket is verified to exist and appears on the discussion with its current state

#### Scenario: Unknown key refused
- **WHEN** a reviewer links a key the tracker does not recognise
- **THEN** the link is refused with the tracker's reason and nothing is recorded as linked

#### Scenario: Unlinking is recorded
- **WHEN** a reviewer unlinks a ticket
- **THEN** the ticket no longer shows as linked and both the linking and the unlinking remain in history

### Requirement: Linked tickets are advisory for approval
The system SHALL list a review's linked tickets with their states and SHALL not prevent approval on
account of open tickets.

#### Scenario: Open tickets do not block approval
- **WHEN** a review has two open linked tickets and its approval gate is otherwise satisfied
- **THEN** the review can reach Approved, with the open tickets listed
