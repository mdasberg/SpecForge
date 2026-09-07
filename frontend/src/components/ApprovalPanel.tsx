import { useState } from 'react';
import type { ApprovalStatus, RequiredReviewer, VerdictType } from '../api/approval';
import { Avatar } from './Avatar';
import { Badge } from './Badge';
import { formatRelativeTime } from '../lib/format';

const REVIEWER_BADGE: Record<RequiredReviewer['state'], { variant: 'draft' | 'approved' | 'changes'; label: string }> = {
  PENDING: { variant: 'draft', label: 'Pending' },
  APPROVED: { variant: 'approved', label: 'Approved' },
  CHANGES_REQUESTED: { variant: 'changes', label: 'Changes requested' },
};

const VERDICT_COPY: Record<VerdictType, { title: string; submit: string }> = {
  APPROVE: { title: 'Approve', submit: 'Submit approval' },
  REQUEST_CHANGES: { title: 'Request changes', submit: 'Submit' },
  COMMENT: { title: 'Comment without a verdict', submit: 'Comment' },
};

/**
 * The review's required reviewers, the approval rule and gate, and the verdict composer. One
 * source of truth for all of it — `GET .../approval` — so the panel never has to reconcile its own
 * idea of "is this satisfied" against the API's.
 */
export function ApprovalPanel({
  status,
  headLabel,
  onCastVerdict,
}: {
  status: ApprovalStatus;
  headLabel: string;
  onCastVerdict: (verdictType: VerdictType, body: string) => Promise<void>;
}) {
  const [open, setOpen] = useState<VerdictType | null>(null);
  const [body, setBody] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { rule, gate, reviewers, verdicts, checks, unresolvedBlockingThreads, composer } = status;
  const secondaryReasons = gate.reasons.filter((reason) => reason !== rule.reason);

  async function submit(verdictType: VerdictType) {
    setBusy(true);
    setError(null);
    try {
      await onCastVerdict(verdictType, body.trim());
      setBody('');
      setOpen(null);
    } catch {
      setError('Could not record that verdict.');
    } finally {
      setBusy(false);
    }
  }

  function canOpen(verdictType: VerdictType): boolean {
    if (verdictType === 'APPROVE') return composer.canApprove;
    if (verdictType === 'REQUEST_CHANGES') return composer.canRequestChanges;
    return composer.canComment;
  }

  return (
    <div className="card" style={{ maxWidth: 340 }}>
      <div className="rp-block">
        <div className="rp-t">Review status</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 9, marginBottom: 10 }}>
          <Badge variant={gate.passed ? 'approved' : 'changes'}>{gate.passed ? 'Ready to approve' : 'Not ready'}</Badge>
          <span className="muted" style={{ fontSize: 11.5 }}>
            {rule.approvedCount} of {rule.requiredCount} required approvals
          </span>
        </div>
        <div className="gate">
          <span style={{ color: rule.satisfied ? 'var(--green)' : 'var(--amber)' }}>{rule.satisfied ? '✓' : '●'}</span>
          <div>{rule.reason}</div>
        </div>
        {secondaryReasons.map((reason) => (
          <div className="gate" style={{ marginTop: 6 }} key={reason}>
            <span style={{ color: 'var(--amber)' }}>●</span>
            <div>{reason}</div>
          </div>
        ))}
      </div>

      <div className="rp-block">
        <div className="rp-t">Reviewers</div>
        {reviewers.map((reviewer) => (
          <div className="rv" key={reviewer.id}>
            <Avatar name={reviewer.reviewer?.displayName ?? '?'} actorKind={reviewer.reviewer?.actorKind} />
            <div style={{ flex: '1 1 auto', minWidth: 0 }}>
              <div className="rv-n">{reviewer.reviewer?.displayName ?? 'Unassigned'}</div>
              <div className="rv-r">
                {reviewer.requiredRole ? `${reviewer.requiredRole} · required` : 'Required reviewer'}
                {reviewer.origin === 'MANUAL' ? ' · added' : ''}
              </div>
            </div>
            <Badge variant={REVIEWER_BADGE[reviewer.state].variant}>
              {REVIEWER_BADGE[reviewer.state].label}
              {reviewer.state === 'APPROVED' ? ` ${headLabel}` : ''}
            </Badge>
          </div>
        ))}
      </div>

      <div className="rp-block">
        <div className="rp-t">
          Unresolved conversations<div className="spacer" />
          <span className="tag">{unresolvedBlockingThreads}</span>
        </div>
        <div className="muted" style={{ fontSize: 11.5 }}>
          {unresolvedBlockingThreads === 0
            ? 'Nothing blocking on the discussion side.'
            : `${unresolvedBlockingThreads} blocking conversation${unresolvedBlockingThreads === 1 ? '' : 's'} — see the Discussions tab.`}
        </div>
      </div>

      <div className="rp-block">
        <div className="rp-t">Automated checks</div>
        {!checks.configured ? (
          <div className="faint" style={{ fontSize: 11.5 }}>Not configured for this project yet.</div>
        ) : (
          <div className="muted" style={{ fontSize: 11.5 }}>
            {checks.failed ? `Failing: ${checks.failedCheckNames.join(', ')}` : 'All checks passed.'}
          </div>
        )}
      </div>

      {verdicts.length > 0 && (
        <div className="rp-block">
          <div className="rp-t">Verdict log</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {verdicts.map((verdict) => (
              <div key={verdict.id} style={{ display: 'flex', gap: 8 }}>
                <Avatar name={verdict.author.displayName} actorKind={verdict.author.actorKind} />
                <div style={{ minWidth: 0 }}>
                  <div className="row-meta" style={{ gap: 6 }}>
                    <b style={{ fontSize: 12 }}>{verdict.author.displayName}</b>
                    <span className="faint">{VERDICT_COPY[verdict.verdictType].title.toLowerCase()}</span>
                    <span className="faint" style={{ fontSize: 11 }}>{formatRelativeTime(verdict.castAt)}</span>
                  </div>
                  {verdict.body && <div className="cmt-t">{verdict.body}</div>}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="rp-block">
        <div style={{ display: 'flex', gap: 6 }}>
          <button
            type="button"
            className="btn btn-primary"
            style={{ flex: '1 1 auto' }}
            disabled={!canOpen('APPROVE')}
            onClick={() => setOpen('APPROVE')}
          >
            Approve
          </button>
          <button
            type="button"
            className="btn btn-warn"
            disabled={!canOpen('REQUEST_CHANGES')}
            onClick={() => setOpen('REQUEST_CHANGES')}
          >
            Request changes
          </button>
        </div>
        <button
          type="button"
          className="btn btn-ghost"
          style={{ width: '100%', marginTop: 6 }}
          disabled={!canOpen('COMMENT')}
          onClick={() => setOpen('COMMENT')}
        >
          Comment without a verdict
        </button>
        {composer.disabledReason && <div className="faint" style={{ fontSize: 11, marginTop: 6 }}>{composer.disabledReason}</div>}

        {open && (
          <div
            style={{
              display: 'flex', flexDirection: 'column', gap: 8, marginTop: 10, padding: 10,
              border: '1px solid var(--indigo)', borderRadius: 8, background: 'var(--panel-2)',
            }}
          >
            <div className="row-meta" style={{ gap: 6 }}>
              <b style={{ color: 'var(--fg)', fontWeight: 600, fontSize: 12 }}>{VERDICT_COPY[open].title}</b>
              <div className="spacer" />
              <span className="tag">{headLabel}</span>
            </div>
            <textarea
              className="input area"
              placeholder={open === 'COMMENT' ? 'Say what you noticed…' : 'Optional note…'}
              value={body}
              onChange={(e) => setBody(e.target.value)}
            />
            {error && <div style={{ color: 'var(--red)', fontSize: 11.5 }}>{error}</div>}
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <button type="button" className="btn btn-sm btn-primary" disabled={busy} onClick={() => void submit(open)}>
                {VERDICT_COPY[open].submit}
              </button>
              <button type="button" className="btn btn-sm btn-ghost" disabled={busy} onClick={() => setOpen(null)}>
                Cancel
              </button>
              <div className="spacer" />
              <span className="faint" style={{ fontSize: 11 }}>recorded in history</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
