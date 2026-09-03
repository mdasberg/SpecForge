## Why

Understanding a change is half the review; arguing about it is the other half. Reviewers need to
select a section — or a phrase inside it — and start a thread there, so that "this error code is
wrong" is attached to the error code and not lost in a comment box at the bottom of a page. Threads
must be resolvable, because an unresolved conversation is what blocks an approval, and they must
remember which version they were written against, because a spec keeps moving.

This change also introduces the authorship rule the product turns on: every comment records whether
a human or an agent wrote it, exposed in the API, so no surface can accidentally present agent
output as a colleague's opinion.

## What Changes

- Anchored comment threads: a thread attaches to a specification version and a section, optionally
  narrowing to a selected text range, and appears both on the document and in a review's discussion
  list.
- Replies within a thread, in order, with @mentions that notify the mentioned reviewer.
- Resolve and reopen, recording who and when; the unresolved count is exposed for approval gating.
- Version anchoring: a thread shows the version it was written against and renders as outdated,
  against its quoted original, when the text it addresses has since changed.
- Author provenance on every comment — human or agent — as an API field, not a UI convention.

## Capabilities

### New Capabilities
- `discussions`: anchored threads, replies, mentions, resolution state, version anchoring and author
  provenance.

### Modified Capabilities
(none)

## Impact

- New module `discussion`; tables for threads, comments and mentions. Threads reference the anchor
  model from `add-spec-review` and re-render on its anchor-state events.
- Notifications: in-app only in this change (a notification list plus unread state); email or Slack
  delivery is deliberately deferred.
- Frontend: comment affordances in the document and diff views, the Discussions tab, and the
  unresolved badge shared with the review panel.
- Depends on: `add-spec-review`.
