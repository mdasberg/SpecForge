import { useState } from 'react';
import type { CheckRun, CheckRunList, CheckRunState, Finding, FindingSeverity } from '../api/agent';
import { Avatar } from './Avatar';
import { Badge } from './Badge';
import { CommentMarkdown } from './CommentMarkdown';
import { EmptyState } from './EmptyState';
import { formatRelativeTime } from '../lib/format';

const STATE_MARK: Record<CheckRunState, { glyph: string; colour: string; label: string }> = {
  QUEUED: { glyph: '○', colour: 'var(--fg-3)', label: 'Queued' },
  RUNNING: { glyph: '◐', colour: 'var(--indigo)', label: 'Running' },
  PASSED: { glyph: '✓', colour: 'var(--green)', label: 'Passed' },
  FAILED: { glyph: '✕', colour: 'var(--red)', label: 'Failed' },
  SKIPPED: { glyph: '–', colour: 'var(--fg-3)', label: 'Skipped' },
};

const SEVERITY_COLOUR: Record<FindingSeverity, string> = {
  INFO: 'var(--fg-3)',
  WARNING: 'var(--amber)',
  ERROR: 'var(--red)',
};

/**
 * A check's duration, once it has one. A run that never started has no duration to show, and
 * showing 0 ms would read as "instant" rather than "not yet".
 */
function duration(run: CheckRun): string | null {
  if (run.durationMs == null) return null;
  return run.durationMs < 1000 ? `${run.durationMs} ms` : `${(run.durationMs / 1000).toFixed(1)} s`;
}

export function ChecksSummaryLine({ summary }: { summary: CheckRunList['summary'] }) {
  if (!summary.configured) return <>Not configured for this project yet.</>;
  const parts = [`${summary.passedCount} of ${summary.totalCount} passed`];
  if (summary.failedCount > 0) parts.push(`${summary.failedCount} failing`);
  if (summary.pendingCount > 0) parts.push(`${summary.pendingCount} running`);
  return <>{parts.join(' · ')}</>;
}

/**
 * One agent finding. It renders through the thread treatment — violet rail, dashed square avatar —
 * because that treatment is what tells a reviewer at a glance that a machine wrote this, and a
 * second presentation for the same kind of content is how that stops being reliable.
 */
function FindingCard({
  finding,
  onAccept,
  onDismiss,
  onDiscuss,
  onUndo,
}: {
  finding: Finding;
  onAccept: () => Promise<void>;
  onDismiss: () => Promise<void>;
  onDiscuss: (body: string) => Promise<void>;
  onUndo: () => Promise<void>;
}) {
  const [draft, setDraft] = useState('');
  const [composing, setComposing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const disposed = finding.disposition ?? null;

  async function act(action: () => Promise<void>) {
    setBusy(true);
    setError(null);
    try {
      await action();
      setDraft('');
      setComposing(false);
    } catch {
      setError('That could not be recorded.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className={`thread agent${disposed ? ' resolved' : ''}`}>
      <div className="th-h" style={{ background: 'var(--violet-dim)' }}>
        <span style={{ color: SEVERITY_COLOUR[finding.severity] }}>◇</span>
        <b>{finding.title}</b>
        <div className="spacer" />
        <span className="tag">{finding.sectionKey}</span>
        <span className="badge b-agent">{finding.severity}</span>
      </div>

      <div className="cmt">
        <Avatar name={finding.author.displayName} actorKind={finding.author.actorKind} />
        <div className="cmt-b">
          <div className="cmt-h">
            <b>{finding.author.displayName}</b>
            <span className="when">
              {finding.checkKey}
              {finding.modelIdentity ? ` · ${finding.modelIdentity}` : ''} · {formatRelativeTime(finding.createdAt)}
            </span>
          </div>
          <div className="cmt-t">
            <CommentMarkdown body={finding.body} mentions={[]} />
          </div>
          {finding.proposedText && (
            <div className="th-anchor" style={{ marginTop: 8, borderLeftColor: 'var(--violet)' }}>
              Suggested: {finding.proposedText}
            </div>
          )}
        </div>
      </div>

      {disposed ? (
        <div className="th-f">
          <span className="faint" style={{ fontSize: 11.5 }}>
            {disposed.kind === 'ACCEPTED' ? 'Accepted' : 'Dismissed'} by {disposed.actor.displayName}{' '}
            {formatRelativeTime(disposed.decidedAt)}
            {disposed.threadId ? ' — a thread carries the suggestion' : ''}
          </span>
          <div className="spacer" />
          <button type="button" className="btn btn-sm btn-ghost" disabled={busy} onClick={() => void act(onUndo)}>
            Undo
          </button>
        </div>
      ) : (
        <div className="th-f" style={{ flexWrap: 'wrap' }}>
          <button type="button" className="btn btn-sm btn-primary" disabled={busy} onClick={() => void act(onAccept)}>
            Accept
          </button>
          <button type="button" className="btn btn-sm btn-ghost" disabled={busy} onClick={() => setComposing(!composing)}>
            Discuss
          </button>
          <button type="button" className="btn btn-sm btn-ghost" disabled={busy} onClick={() => void act(onDismiss)}>
            Dismiss
          </button>
          <div className="spacer" />
          <span className="faint" style={{ fontSize: 11 }}>
            accepting opens a thread; it never edits the spec
          </span>
        </div>
      )}

      {composing && !disposed && (
        <div className="reply" style={{ flexDirection: 'column', alignItems: 'stretch', gap: 8 }}>
          <textarea
            className="input area"
            placeholder="Why this finding is right, or why it is not…"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
          />
          {error && <div style={{ color: 'var(--red)', fontSize: 11.5 }}>{error}</div>}
          <div style={{ display: 'flex', gap: 6 }}>
            <button
              type="button"
              className="btn btn-sm btn-primary"
              disabled={busy || !draft.trim()}
              onClick={() => void act(() => onDiscuss(draft.trim()))}
            >
              Open thread
            </button>
            <button type="button" className="btn btn-sm btn-ghost" disabled={busy} onClick={() => setComposing(false)}>
              Cancel
            </button>
          </div>
        </div>
      )}
      {error && !composing && <div className="th-f" style={{ color: 'var(--red)', fontSize: 11.5 }}>{error}</div>}
    </div>
  );
}

/**
 * The Checks tab: every check against the review's current head, what it concluded, and the findings
 * it produced. Blocking is marked on the check rather than on the finding, because that is where the
 * project sets it — an agent's severity never decides whether an approval is stopped.
 */
export function ChecksPanel({
  checks,
  onRerunAll,
  onRerun,
  onAccept,
  onDismiss,
  onDiscuss,
  onUndo,
}: {
  checks: CheckRunList;
  onRerunAll: () => Promise<void>;
  onRerun: (checkRunId: string) => Promise<void>;
  onAccept: (findingId: string) => Promise<void>;
  onDismiss: (findingId: string) => Promise<void>;
  onDiscuss: (findingId: string, body: string) => Promise<void>;
  onUndo: (findingId: string) => Promise<void>;
}) {
  const [busy, setBusy] = useState(false);

  async function rerun(action: () => Promise<void>) {
    setBusy(true);
    try {
      await action();
    } finally {
      setBusy(false);
    }
  }

  if (checks.items.length === 0) {
    return (
      <EmptyState title="No checks have run">
        Checks are dispatched when a review's head arrives or advances. If this project has none
        enabled, nothing here is waiting on them.
      </EmptyState>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <span className="muted" style={{ fontSize: 12 }}>
          <ChecksSummaryLine summary={checks.summary} />
        </span>
        {checks.summary.failed && <Badge variant="blocked">Blocking check failed</Badge>}
        <div className="spacer" />
        <button type="button" className="btn btn-sm btn-ghost" disabled={busy} onClick={() => void rerun(onRerunAll)}>
          Rerun all
        </button>
      </div>

      {checks.items.map((run) => (
        <div className="card" key={run.id}>
          <div className="rp-block">
            <div className="check">
              <span style={{ color: STATE_MARK[run.state].colour }}>{STATE_MARK[run.state].glyph}</span>
              <div style={{ flex: '1 1 auto', minWidth: 0 }}>
                <div className="nm">
                  {run.displayName}
                  {run.blocking ? <span className="tag" style={{ marginLeft: 7 }}>blocking</span> : null}
                  {run.runnerKind === 'MODEL' ? <span className="badge b-agent" style={{ marginLeft: 7 }}>model</span> : null}
                </div>
                <div className="ds">
                  {STATE_MARK[run.state].label} · {run.headLabel}
                  {duration(run) ? ` · ${duration(run)}` : ''}
                  {run.summary ? ` · ${run.summary}` : ''}
                </div>
              </div>
              <button
                type="button"
                className="btn btn-sm btn-ghost"
                disabled={busy}
                onClick={() => void rerun(() => onRerun(run.id))}
              >
                Rerun
              </button>
            </div>
          </div>

          {run.findings.length > 0 && (
            <div className="rp-block" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {run.findings.map((finding) => (
                <FindingCard
                  key={finding.id}
                  finding={finding}
                  onAccept={() => onAccept(finding.id)}
                  onDismiss={() => onDismiss(finding.id)}
                  onDiscuss={(body) => onDiscuss(finding.id, body)}
                  onUndo={() => onUndo(finding.id)}
                />
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
