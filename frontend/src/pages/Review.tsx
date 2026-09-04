import { useEffect, useMemo, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router';
import { useAuth } from 'react-oidc-context';
import { getReview, getReviewDiff } from '../api/review';
import type { Review as ReviewDetail, SpecDiff } from '../api/review';
import { ApiError } from '../auth/api';
import { DiffJumpList, DiffModeToggle, DiffSummaryLine, DiffView } from '../components/DiffView';
import { useDiffMode } from '../lib/useDiffMode';
import { SpecMarkdown } from '../components/SpecMarkdown';
import { StatusBadge } from '../components/StatusBadge';
import { formatRelativeTime } from '../lib/format';

type Tab = 'document' | 'changes';

/**
 * One review. The shell is deliberately built around tabs even though only two of them exist: the
 * later changes — discussions, checks, history, traceability — are tabs of this same screen, and a
 * layout that has to be rebuilt to accept them is a layout that will be rebuilt wrongly.
 */
export function Review() {
  const { reviewId } = useParams<{ reviewId: string }>();
  const auth = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const tab: Tab = searchParams.get('tab') === 'changes' ? 'changes' : 'document';

  const [review, setReview] = useState<ReviewDetail | null>(null);
  const [diff, setDiff] = useState<SpecDiff | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [mode, setMode] = useDiffMode();

  useEffect(() => {
    if (!reviewId) return;
    let cancelled = false;
    async function run() {
      setLoading(true);
      setError(null);
      try {
        const result = await getReview(reviewId as string, auth.user);
        if (!cancelled) setReview(result);
      } catch (e) {
        if (cancelled) return;
        if (e instanceof ApiError) setError(e.problem.detail ?? e.problem.title ?? 'Could not load this review.');
        else throw e;
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    void run();
    return () => {
      cancelled = true;
    };
  }, [reviewId, auth.user]);

  // The diff is fetched only once the Changes tab is opened: a reviewer who reads the document and
  // leaves never pays for it.
  useEffect(() => {
    if (!reviewId || tab !== 'changes' || diff) return;
    let cancelled = false;
    async function run() {
      try {
        const result = await getReviewDiff(reviewId as string, auth.user);
        if (!cancelled) setDiff(result);
      } catch (e) {
        if (cancelled) return;
        if (e instanceof ApiError) setError(e.problem.detail ?? e.problem.title ?? 'Could not load the diff.');
        else throw e;
      }
    }
    void run();
    return () => {
      cancelled = true;
    };
  }, [reviewId, tab, diff, auth.user]);

  const anchors = useMemo(() => (review ? review.sections.map((section) => section.anchorKey) : []), [review]);

  function selectTab(next: Tab) {
    setSearchParams((prev) => {
      const params = new URLSearchParams(prev);
      params.set('tab', next);
      return params;
    });
  }

  if (loading && !review) return <div className="card card-pad">Loading review…</div>;

  if (error && !review) {
    return (
      <div className="card card-pad">
        <div className="card-t">Could not load review</div>
        <p style={{ color: 'var(--fg-2)', margin: '8px 0 0' }}>{error}</p>
      </div>
    );
  }

  if (!review) return null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div>
        <div className="row-meta" style={{ marginBottom: 6 }}>
          <span>{review.spec.project}</span>
          <span className="sep">/</span>
          <Link to={`/specs/${review.spec.id}`} className="mono faint">{review.spec.path}</Link>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
          <h1 className="h1">{review.spec.title}</h1>
          <StatusBadge status={review.spec.status} />
          <span className="tag">{review.base.label} → {review.head.label}</span>
          {review.state === 'CLOSED' && <span className="badge b-impl">Closed</span>}
        </div>
        <div className="row-meta" style={{ marginTop: 5, flexWrap: 'wrap' }}>
          {review.pullRequest && (
            <>
              <span className="mono">
                {review.pullRequest.repositoryFullName} #{review.pullRequest.number}
              </span>
              <span className="sep">·</span>
            </>
          )}
          <span>opened {formatRelativeTime(review.openedAt)}{review.openedBy ? ` by ${review.openedBy}` : ''}</span>
          <span className="sep">·</span>
          {/* Reviewers arrive with the approval capability; saying so is more honest than an empty
              avatar stack that looks like nobody was asked. */}
          <span className="faint">No reviewers assigned yet</span>
        </div>
      </div>

      <div className="tabs">
        <button type="button" className={`tab${tab === 'document' ? ' on' : ''}`} onClick={() => selectTab('document')}>
          Document
        </button>
        <button type="button" className={`tab${tab === 'changes' ? ' on' : ''}`} onClick={() => selectTab('changes')}>
          Changes
          {diff && <span className="count">{diff.summary.changedLines}</span>}
        </button>
      </div>

      {tab === 'document' ? (
        <SpecMarkdown content={review.content} anchors={anchors} />
      ) : !diff ? (
        <div className="card card-pad">{error ?? 'Loading changes…'}</div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: '200px minmax(0, 1fr)', gap: 20, alignItems: 'start' }}>
          <aside>
            <DiffJumpList diff={diff} />
          </aside>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, minWidth: 0 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <DiffSummaryLine diff={diff} />
              <span className="spacer" />
              <DiffModeToggle mode={mode} onChange={setMode} />
            </div>
            <DiffView diff={diff} mode={mode} />
          </div>
        </div>
      )}
    </div>
  );
}
