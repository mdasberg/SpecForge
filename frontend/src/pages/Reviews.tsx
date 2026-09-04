import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { useAuth } from 'react-oidc-context';
import { listReviews } from '../api/review';
import type { ReviewList, ReviewState, ReviewSummary } from '../api/review';
import { ApiError } from '../auth/api';
import { Button } from '../components/Button';
import { EmptyState } from '../components/EmptyState';
import { StatusBadge } from '../components/StatusBadge';
import { formatRelativeTime } from '../lib/format';

const STATES: { value: ReviewState | 'ALL'; label: string }[] = [
  { value: 'OPEN', label: 'Open' },
  { value: 'CLOSED', label: 'Closed' },
  { value: 'ALL', label: 'All' },
];

function ReviewRow({ review, onOpen }: { review: ReviewSummary; onOpen: () => void }) {
  return (
    <div
      className="row click"
      role="button"
      tabIndex={0}
      onClick={onOpen}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onOpen();
        }
      }}
    >
      <div style={{ flex: '1 1 auto', minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span className="row-t">{review.spec.title}</span>
          <span className="tag">{review.base.label} → {review.head.label}</span>
        </div>
        <div className="faint mono" style={{ fontSize: 11, marginTop: 1 }}>{review.spec.path}</div>
      </div>
      <StatusBadge status={review.spec.status} />
      {review.pullRequest && (
        <span className="faint mono" style={{ fontSize: 11.5, width: 64, textAlign: 'right' }}>
          #{review.pullRequest.number}
        </span>
      )}
      <span className="faint tnum" style={{ fontSize: 11.5, width: 64, textAlign: 'right' }}>
        {formatRelativeTime(review.updatedAt)}
      </span>
    </div>
  );
}

export function Reviews() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [state, setState] = useState<ReviewState | 'ALL'>('OPEN');
  const [list, setList] = useState<ReviewList | null>(null);
  const [items, setItems] = useState<ReviewSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function run() {
      setLoading(true);
      setError(null);
      try {
        const result = await listReviews({ state: state === 'ALL' ? undefined : state, limit: 50 }, auth.user);
        if (cancelled) return;
        setList(result);
        setItems(result.items);
      } catch (e) {
        if (cancelled) return;
        if (e instanceof ApiError) setError(e.problem.detail ?? e.problem.title ?? 'Could not load reviews.');
        else throw e;
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    void run();
    return () => {
      cancelled = true;
    };
  }, [state, auth.user]);

  async function loadMore() {
    if (!list?.cursor) return;
    setLoadingMore(true);
    try {
      const result = await listReviews(
        { state: state === 'ALL' ? undefined : state, limit: 50, cursor: list.cursor },
        auth.user,
      );
      setList(result);
      setItems((prev) => [...prev, ...result.items]);
    } catch (e) {
      if (e instanceof ApiError) setError(e.problem.detail ?? e.problem.title ?? 'Could not load more reviews.');
      else throw e;
    } finally {
      setLoadingMore(false);
    }
  }

  if (loading && !list) return <div className="card card-pad">Loading reviews…</div>;

  if (error) {
    return (
      <div className="card card-pad">
        <div className="card-t">Could not load reviews</div>
        <p style={{ color: 'var(--fg-2)', margin: '8px 0 0' }}>{error}</p>
      </div>
    );
  }

  if (list && list.total === 0 && state === 'OPEN') {
    return (
      <EmptyState title="No reviews yet">
        A review opens when a pull request touches a specification. Connect a repository to start.
      </EmptyState>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div style={{ display: 'flex', alignItems: 'flex-end', gap: 12 }}>
        <div>
          <h1 className="h1">Reviews</h1>
          <p className="sub">
            {list?.total ?? 0} review{list?.total === 1 ? '' : 's'}
            {loading && ' · updating…'}
          </p>
        </div>
        <div className="spacer" />
        <div style={{ display: 'flex', gap: 4 }}>
          {STATES.map((option) => (
            <button
              key={option.value}
              type="button"
              className={`chip${state === option.value ? ' on' : ''}`}
              onClick={() => setState(option.value)}
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>

      {items.length === 0 ? (
        <div className="card card-pad">
          <div className="card-t">No reviews match</div>
        </div>
      ) : (
        <div className="card">
          <div className="rows">
            {items.map((review) => (
              <ReviewRow key={review.id} review={review} onOpen={() => navigate(`/reviews/${review.id}`)} />
            ))}
          </div>
        </div>
      )}

      {list?.cursor && (
        <Button onClick={() => void loadMore()} disabled={loadingMore}>
          {loadingMore ? 'Loading…' : 'Load more'}
        </Button>
      )}
    </div>
  );
}
