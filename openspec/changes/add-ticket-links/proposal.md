## Why

Review comments that require work have to leave the review, or they rot. The designed move is one
click from a discussion: a compact modal with title, description, project, priority and assignee,
creating a ticket in whichever tracker the team already uses, with a link back to the exact
conversation that caused it. Afterwards the discussion shows its ticket, so the next reader sees
that the objection was actioned rather than ignored.

No tracker may be privileged in the model: GitHub Issues, Jira and Linear are three
interchangeable implementations of the same port.

## What Changes

- Create a ticket from a comment: modal prefilled from the comment, capturing title, description,
  project, priority and assignee.
- Tracker adapters behind one port, with GitHub Issues, Jira and Linear as peers; a project
  configures one tracker.
- Two-way linkage: the created ticket carries a deep link back to the discussion, and the discussion
  shows the ticket with its key, title, status and assignee, refreshed by webhook where available
  and by poll otherwise.
- Link an existing ticket to a discussion, and unlink.
- Linked tickets and their states listed on the review, advisory only — open tickets do not block approval.
- With no tracker configured, the action is offered as disabled with the reason, not hidden.

## Capabilities

### New Capabilities
- `ticket-links`: ticket creation from a discussion, tracker adapters, and the two-way link between a
  discussion and a tracker ticket.

### Modified Capabilities
(none)

## Impact

- New module `ticket`; tables for tracker configurations, ticket links and cached ticket state.
- Outbound integrations: GitHub Issues (reusing the existing App installation), Jira Cloud and Linear,
  each needing its own credential storage; inbound webhooks for status refresh.
- Frontend: the create-ticket modal and the linked-ticket presentation inside a thread.
- Depends on: `add-review-discussions`.
