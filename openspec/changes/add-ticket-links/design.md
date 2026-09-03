## Context

Three trackers, one product surface, and a strong pull towards building for whichever one is wired
up first. The design brief is explicit that none may be fundamental.

## Goals / Non-Goals

**Goals:**
- The create-ticket flow is identical whichever tracker is behind it.
- A reader of the discussion can see the ticket's current state without leaving.
- A ticket outliving its review still points back at why it exists.

**Non-Goals:**
- Two-way field sync, comment mirroring, or editing a ticket from SpecForge.
- Multiple trackers per project.
- Sprint, epic or estimate management.

## Decisions

- **One `Tracker` port with a narrow contract: create, fetch, search.** Every adapter maps SpecForge's
  five fields onto its own vocabulary. Anything a tracker cannot express (Linear has no priority
  names matching Jira's) is mapped in the adapter, never leaked into the modal.
- **The link is SpecForge's own row, keyed by tracker plus external key.** The tracker is not the
  source of truth for the link; deleting a ticket in Jira leaves the link visible as unavailable
  rather than losing the fact that a ticket was created.
- **The backlink is written into the ticket body at creation, once.** A permalink to the thread, in
  text. No app, no plugin, no dependency on SpecForge being reachable to make sense of the ticket.
- **Ticket state is cached, refreshed by webhook where available and polled otherwise, with the
  cache's age shown.** A stale status displayed as current is worse than a stale status labelled as such.
- **Linked tickets are advisory for approval.** Requiring closed tickets before approving a spec
  inverts the order of work: the spec is approved, then implemented.
- **No tracker configured disables the action with a reason.** A hidden feature reads as a missing
  one; a disabled one with "no tracker configured for this project" points at the fix.

## Risks / Trade-offs

- Three adapters is three credential stores and three sets of API quirks. Mitigated by the narrow
  port and by GitHub reusing the App installation already present.
- Priority and assignee vocabularies differ enough that a mapping will occasionally surprise. The
  modal shows the tracker's own values once a tracker is configured, rather than an invented set.
- Polling for status has a cost per linked ticket. Bounded: poll only tickets linked to open reviews,
  and only where no webhook is configured.
