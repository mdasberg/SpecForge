## 1. Threads and comments

- [x] 1.1 Tables: `thread` (specification, version, section key, optional text range, quoted original,
      review, resolution state), `comment` (thread, author, actor kind, body, created at).
- [~] 1.2 Create a thread from a section, or from a text selection within a section, in both the
      document and the diff view. API and the Discussions tab's composer support both anchor kinds;
      there is no inline click-to-comment affordance in the rendered document or diff yet — see hot.md.
- [x] 1.3 Reply to a thread; replies are ordered and immutable once posted apart from edit-by-author
      within a short window (`PUT /api/threads/{id}/comments/{id}`, 15-minute window).
- [x] 1.4 Markdown in comment bodies, rendered with the same renderer as specification text.

## 2. Mentions and notifications

- [x] 2.1 Parse `@handle` mentions against project members; store them as rows, not by re-parsing text.
- [~] 2.2 In-app notification per mention and per reply on a thread the user participates in. "Per
      thread opened on a specification the user owns" is deferred — `SpecDocument.owner` is a free-text
      string, not a subject id, and matching it would be exactly the fuzzy reattachment this codebase
      avoids elsewhere. Needs a real owner→identity mapping first.
- [x] 2.3 Notification list with unread state and mark-as-read (bell in the top bar).

## 3. Resolution

- [x] 3.1 Resolve and reopen a thread, recording actor and timestamp each time.
- [x] 3.2 Expose the unresolved thread count per review, distinguishing blocking from non-blocking
      threads (derived from the opener's actor kind — agents never approve, so an agent-opened thread
      never blocks).
- [x] 3.3 Resolution state is shared across surfaces: resolving refetches the thread list in the same
      render cycle, so the Discussions tab and its unresolved badge agree immediately.

## 4. Version anchoring

- [x] 4.1 Render each thread with the version it was written against and its current anchor state
      (current, stale, orphaned).
- [~] 4.2 Show the quoted original for stale and orphaned threads. No clickable link to the version
      itself yet — the Thread DTO doesn't carry a document id to link from; small follow-up.
- [x] 4.3 Re-render on the head-updated event from `add-spec-review` without losing thread content.

## 5. Provenance

- [x] 5.1 Every comment carries the author's actor kind; the API exposes it on every read.
- [x] 5.2 Frontend renders agent authorship distinctly per the design system (square dashed avatar,
      violet rail) — reuses the existing `Avatar`/`.thread.agent`/`.b-agent` treatment.
- [x] 5.3 Contract test asserting no comment response can omit actor kind.

## 6. Verification

- [x] 6.1 Tests: thread on a text range survives an unrelated edit; becomes stale on an edit to its text;
      becomes orphaned when its section disappears.
- [x] 6.2 Test: resolving a thread decrements the review's unresolved count and reopening increments it.
