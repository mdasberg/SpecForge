## 1. Threads and comments

- [ ] 1.1 Tables: `thread` (specification, version, section key, optional text range, quoted original,
      review, resolution state), `comment` (thread, author, actor kind, body, created at).
- [ ] 1.2 Create a thread from a section, or from a text selection within a section, in both the
      document and the diff view.
- [ ] 1.3 Reply to a thread; replies are ordered and immutable once posted apart from edit-by-author
      within a short window.
- [ ] 1.4 Markdown in comment bodies, rendered with the same renderer as specification text.

## 2. Mentions and notifications

- [ ] 2.1 Parse `@handle` mentions against project members; store them as rows, not by re-parsing text.
- [ ] 2.2 In-app notification per mention, per reply on a thread the user participates in, and per
      thread opened on a specification the user owns.
- [ ] 2.3 Notification list with unread state and mark-as-read.

## 3. Resolution

- [ ] 3.1 Resolve and reopen a thread, recording actor and timestamp each time.
- [ ] 3.2 Expose the unresolved thread count per review, distinguishing blocking from non-blocking threads.
- [ ] 3.3 Resolution state is shared across surfaces: resolving in the document updates the Discussions
      tab and the review panel count in the same response cycle.

## 4. Version anchoring

- [ ] 4.1 Render each thread with the version it was written against and its current anchor state
      (current, stale, orphaned).
- [ ] 4.2 Show the quoted original for stale and orphaned threads, with a link to the version where it applied.
- [ ] 4.3 Re-render on the head-updated event from `add-spec-review` without losing thread content.

## 5. Provenance

- [ ] 5.1 Every comment carries the author's actor kind; the API exposes it on every read.
- [ ] 5.2 Frontend renders agent authorship distinctly per the design system (square dashed avatar,
      violet rail) — never only by name.
- [ ] 5.3 Contract test asserting no comment response can omit actor kind.

## 6. Verification

- [ ] 6.1 Tests: thread on a text range survives an unrelated edit; becomes stale on an edit to its text;
      becomes orphaned when its section disappears.
- [ ] 6.2 Test: resolving a thread decrements the review's unresolved count and reopening increments it.
