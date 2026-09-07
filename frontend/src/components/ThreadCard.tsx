import { useState } from 'react';
import type { Thread } from '../api/discussion';
import { Avatar } from './Avatar';
import { CommentMarkdown } from './CommentMarkdown';
import { formatRelativeTime } from '../lib/format';

const ANCHOR_LABEL: Record<Thread['anchorState'], string> = {
  CURRENT: '',
  STALE: 'Outdated — text has changed since',
  ORPHANED: 'Orphaned — this section is gone',
};

/**
 * One thread: its anchor, every comment in order, and the resolve/reopen/reply affordances. Used
 * both by the Discussions tab and, later, the document's comment gutter — one component, so
 * resolving a thread never renders differently depending on where it was resolved from.
 */
export function ThreadCard({
  thread,
  onReply,
  onResolve,
  onReopen,
  busy,
}: {
  thread: Thread;
  onReply: (body: string) => Promise<void>;
  onResolve: () => Promise<void>;
  onReopen: () => Promise<void>;
  busy: boolean;
}) {
  const [draft, setDraft] = useState('');
  const isAgent = thread.opener.actorKind === 'AGENT';
  const anchorNote = ANCHOR_LABEL[thread.anchorState];

  async function submitReply() {
    if (!draft.trim()) return;
    await onReply(draft.trim());
    setDraft('');
  }

  return (
    <div className={`thread${isAgent ? ' agent' : ''}${thread.resolved ? ' resolved' : ''}`}>
      <div className="th-h" style={isAgent ? { background: 'var(--violet-dim)' } : undefined}>
        <span style={{ color: thread.resolved ? 'var(--green)' : isAgent ? 'var(--violet)' : 'var(--amber)' }}>
          {thread.resolved ? '✓' : isAgent ? '◇' : '●'}
        </span>
        <b>{thread.comments[0]?.body.slice(0, 80) ?? thread.quotedText}</b>
        <div className="spacer" />
        <span className="tag">{thread.sectionKey}</span>
        {!thread.blocking && <span className="badge b-agent">Non-blocking</span>}
      </div>

      {anchorNote && (
        <div className="th-anchor">
          {anchorNote}: written against {thread.anchorVersionLabel} — “{thread.quotedText}”
        </div>
      )}

      {thread.comments.map((comment) => (
        <div className="cmt" key={comment.id}>
          <Avatar name={comment.author.displayName} actorKind={comment.author.actorKind} />
          <div className="cmt-b">
            <div className="cmt-h">
              <b>{comment.author.displayName}</b>
              <span className="when">
                {formatRelativeTime(comment.createdAt)}
                {comment.editedAt ? ' · edited' : ''}
              </span>
            </div>
            <CommentMarkdown body={comment.body} mentions={comment.mentions} />
          </div>
        </div>
      ))}

      <div className="th-f">
        <button
          type="button"
          className="btn btn-sm"
          disabled={busy}
          onClick={() => void (thread.resolved ? onReopen() : onResolve())}
        >
          {thread.resolved ? 'Reopen' : 'Resolve'}
        </button>
        {thread.resolved && thread.resolvedBy && (
          <span className="faint" style={{ fontSize: 11 }}>
            resolved by {thread.resolvedBy.displayName}
          </span>
        )}
        <div className="spacer" />
        <span className="faint" style={{ fontSize: 11 }}>
          {thread.comments.length} {thread.comments.length === 1 ? 'comment' : 'comments'}
        </span>
      </div>

      <div className="reply">
        <input
          className="input"
          style={{ flex: '1 1 auto' }}
          placeholder="Reply…"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              void submitReply();
            }
          }}
          disabled={busy}
        />
        <button type="button" className="btn btn-sm" disabled={busy || !draft.trim()} onClick={() => void submitReply()}>
          Reply
        </button>
      </div>
    </div>
  );
}
