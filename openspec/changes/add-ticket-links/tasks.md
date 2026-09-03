## 1. Tracker port and configuration

- [ ] 1.1 `Tracker` port: `create(ticket)`, `fetch(key)`, `search(query)`; one result shape for all adapters.
- [ ] 1.2 Per-project tracker configuration with credential storage, connection test and an audited change trail.
- [ ] 1.3 GitHub Issues adapter reusing the App installation.
- [ ] 1.4 Jira Cloud adapter (project, issue type, priority, assignee lookup).
- [ ] 1.5 Linear adapter (team, priority, assignee lookup).

## 2. Create from a comment

- [ ] 2.1 Modal prefilled from the comment: title from its first line, description from its body plus
      the quoted spec text and a permalink.
- [ ] 2.2 Project, priority and assignee choices fetched from the configured tracker, not hardcoded.
- [ ] 2.3 Create the ticket, persist the link, and post the result into the thread as a system entry.
- [ ] 2.4 Handle creation failure without losing the entered values, surfacing the tracker's message.
- [ ] 2.5 Disable the action with an explanatory reason when the project has no tracker configured.

## 3. Linkage and state

- [ ] 3.1 `ticket_link` (thread, tracker, external key, url, created by, created at) plus cached
      `ticket_state` (title, status, assignee, fetched at).
- [ ] 3.2 Link an existing ticket by key or URL after validating it exists; unlink, keeping history.
- [ ] 3.3 Refresh cached state by inbound webhook where the tracker supports it.
- [ ] 3.4 Poll tickets linked to open reviews where no webhook exists; show the cache age.
- [ ] 3.5 Render an unavailable or deleted ticket as unavailable rather than dropping the link.

## 4. Presentation

- [ ] 4.1 Linked ticket inside the thread: key, title, status, assignee, tracker icon, deep link.
- [ ] 4.2 Review-level list of linked tickets with their states, marked advisory.

## 5. Verification

- [ ] 5.1 Adapter contract tests run against all three adapters over the same scenarios.
- [ ] 5.2 Tests: created ticket body contains a resolvable permalink to the thread; a deleted remote
      ticket renders as unavailable; open tickets do not block approval.
