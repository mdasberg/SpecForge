import { useEffect, useMemo, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router';
import { useAuth } from 'react-oidc-context';
import { getReview, getReviewDiff } from '../api/review';
import type { Review as ReviewDetail, SpecDiff } from '../api/review';
import { listThreads, openThread, replyToThread, reopenThread, resolveThread } from '../api/discussion';
import type { ThreadList } from '../api/discussion';
import { castVerdict, getApprovalStatus } from '../api/approval';
import type { ApprovalStatus, VerdictType } from '../api/approval';
import {
  acceptFinding,
  discussFinding,
  dismissFinding,
  listChecks,
  rerunCheck,
  rerunChecks,
  undoFindingDisposition,
} from '../api/agent';
import type { CheckRunList } from '../api/agent';
import { ApiError } from '../auth/api';
import { ApprovalPanel } from '../components/ApprovalPanel';
import { ChecksPanel } from '../components/ChecksPanel';
import { DiffJumpList, DiffModeToggle, DiffSummaryLine, DiffView } from '../components/DiffView';
import { useDiffMode } from '../lib/useDiffMode';
import { EmptyState } from '../components/EmptyState';
import { NewThreadForm } from '../components/NewThreadForm';
import { SpecMarkdown } from '../components/SpecMarkdown';
import { StatusBadge } from '../components/StatusBadge';
import { ThreadCard } from '../components/ThreadCard';
import { formatRelativeTime } from '../lib/format';

type Tab = 'document' | 'changes' | 'discussions' | 'checks' | 'approval';

/**
 * One review. The shell is deliberately built around tabs even though only two of them exist: the
 * later changes — discussions, checks, history, traceability — are tabs of this same screen, and a
 * layout that has to be rebuilt to accept them is a layout that will be rebuilt wrongly.
 */
export function Review() {
  const { reviewId } = useParams<{ reviewId: string }>();
  const auth = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const tabParam = searchParams.get('tab');
  const tab: Tab =
    tabParam === 'changes' ? 'changes'
    : tabParam === 'discussions' ? 'discussions'
    : tabParam === 'checks' ? 'checks'
    : tabParam === 'approval' ? 'approval'
    : 'document';

  const [review, setReview] = useState<ReviewDetail | null>(null);
  const [diff, setDiff] = useState<SpecDiff | null>(null);
  const [threads, setThreads] = useState<ThreadList | null>(null);
  const [approval, setApproval] = useState<ApprovalStatus | null>(null);
  const [checks, setChecks] = useState<CheckRunList | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [mode, setMode] = useDiffMode();

  async function refreshThreads() {
    if (!reviewId) return;
    setThreads(await listThreads(reviewId, auth.user));
  }

  useEffect(() => {
    if (!reviewId) return;
    let cancelled = false;
    listThreads(reviewId, auth.user)
      .then((result) => {
        if (!cancelled) setThreads(result);
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, [reviewId, auth.user]);

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

  // Same reasoning as the diff: the approval panel reconciles the review's seats against the
  // project's live rule on every fetch, so it is worth fetching only when someone looks at it.
  useEffect(() => {
    if (!reviewId || tab !== 'approval' || approval) return;
    let cancelled = false;
    async function run() {
      try {
        const result = await getApprovalStatus(reviewId as string, auth.user);
        if (!cancelled) setApproval(result);
      } catch (e) {
        if (cancelled) return;
        if (e instanceof ApiError) setError(e.problem.detail ?? e.problem.title ?? 'Could not load the approval status.');
        else throw e;
      }
    }
    void run();
    return () => {
      cancelled = true;
    };
  }, [reviewId, tab, approval, auth.user]);

  // Same reasoning as the diff and the approval panel: fetched when someone opens the tab. Checks
  // keep running after the page loaded, so this refetches on every visit rather than caching once.
  useEffect(() => {
    if (!reviewId || tab !== 'checks') return;
    let cancelled = false;
    async function run() {
      try {
        const result = await listChecks(reviewId as string, auth.user);
        if (!cancelled) setChecks(result);
      } catch (e) {
        if (cancelled) return;
        if (e instanceof ApiError) setError(e.problem.detail ?? e.problem.title ?? 'Could not load the checks.');
        else throw e;
      }
    }
    void run();
    return () => {
      cancelled = true;
    };
  }, [reviewId, tab, auth.user]);

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
        <button type="button" className={`tab${tab === 'discussions' ? ' on' : ''}`} onClick={() => selectTab('discussions')}>
          Discussions
          {threads && (threads.unresolvedBlockingCount + threads.unresolvedNonBlockingCount > 0) && (
            <span className="count">{threads.unresolvedBlockingCount + threads.unresolvedNonBlockingCount}</span>
          )}
        </button>
        <button type="button" className={`tab${tab === 'checks' ? ' on' : ''}`} onClick={() => selectTab('checks')}>
          Checks
          {checks && checks.summary.configured && (
            <span className="count">
              {checks.summary.passedCount}/{checks.summary.totalCount}
            </span>
          )}
        </button>
        <button type="button" className={`tab${tab === 'approval' ? ' on' : ''}`} onClick={() => selectTab('approval')}>
          Approval
          {approval && <span className="count">{approval.rule.approvedCount}/{approval.rule.requiredCount}</span>}
        </button>
      </div>

      {tab === 'document' ? (
        <SpecMarkdown content={review.content} anchors={anchors} />
      ) : tab === 'approval' ? (
        !approval ? (
          <div className="card card-pad">{error ?? 'Loading approval status…'}</div>
        ) : (
          <ApprovalPanel
            status={approval}
            headLabel={review.head.label}
            onCastVerdict={async (verdictType: VerdictType, body: string) => {
              const result = await castVerdict(
                reviewId as string,
                { verdictType, body: body || undefined, atHeadSha: review.head.contentSha },
                auth.user,
              );
              setApproval(result);
              setReview(await getReview(reviewId as string, auth.user));
            }}
          />
        )
      ) : tab === 'checks' ? (
        !checks ? (
          <div className="card card-pad">{error ?? 'Loading checks…'}</div>
        ) : (
          <ChecksPanel
            checks={checks}
            onRerunAll={async () => setChecks(await rerunChecks(reviewId as string, auth.user))}
            onRerun={async (checkRunId) => setChecks(await rerunCheck(checkRunId, auth.user))}
            onAccept={async (findingId) => {
              await acceptFinding(findingId, auth.user);
              // Accepting opened a thread, so the Discussions tab and the approval gate both moved.
              setChecks(await listChecks(reviewId as string, auth.user));
              await refreshThreads();
              setApproval(null);
            }}
            onDismiss={async (findingId) => {
              await dismissFinding(findingId, auth.user);
              setChecks(await listChecks(reviewId as string, auth.user));
            }}
            onDiscuss={async (findingId, body) => {
              await discussFinding(findingId, body, auth.user);
              setChecks(await listChecks(reviewId as string, auth.user));
              await refreshThreads();
            }}
            onUndo={async (findingId) => {
              await undoFindingDisposition(findingId, auth.user);
              setChecks(await listChecks(reviewId as string, auth.user));
            }}
          />
        )
      ) : tab === 'discussions' ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <NewThreadForm
            sections={review.sections}
            content={review.content}
            onSubmit={async (request) => {
              await openThread(reviewId as string, request, auth.user);
              await refreshThreads();
            }}
          />
          {threads && threads.items.length === 0 && (
            <EmptyState title="No discussions yet">
              Select a section above, or quote an exact phrase, to start one.
            </EmptyState>
          )}
          {threads?.items.map((thread) => (
            <ThreadCard
              key={thread.id}
              thread={thread}
              busy={false}
              onReply={async (body) => {
                await replyToThread(thread.id, body, auth.user);
                await refreshThreads();
              }}
              onResolve={async () => {
                await resolveThread(thread.id, auth.user);
                await refreshThreads();
              }}
              onReopen={async () => {
                await reopenThread(thread.id, auth.user);
                await refreshThreads();
              }}
            />
          ))}
        </div>
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
