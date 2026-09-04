import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router';
import { useAuth } from 'react-oidc-context';
import { compareVersions } from '../api/review';
import type { SpecDiff } from '../api/review';
import { ApiError } from '../auth/api';
import { DiffJumpList, DiffModeToggle, DiffSummaryLine, DiffView } from '../components/DiffView';
import { useDiffMode } from '../lib/useDiffMode';

/**
 * Comparing two versions out of curiosity. It renders the same diff a review does, and creates no
 * review, no reviewers and no review history — looking is not reviewing.
 */
export function CompareVersions() {
  const { specId } = useParams<{ specId: string }>();
  const [searchParams] = useSearchParams();
  const auth = useAuth();
  const base = Number(searchParams.get('base') ?? 1);
  const head = Number(searchParams.get('head') ?? 1);

  const [diff, setDiff] = useState<SpecDiff | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [mode, setMode] = useDiffMode();

  useEffect(() => {
    if (!specId) return;
    let cancelled = false;
    async function run() {
      setError(null);
      try {
        const result = await compareVersions(specId as string, base, head, auth.user);
        if (!cancelled) setDiff(result);
      } catch (e) {
        if (cancelled) return;
        if (e instanceof ApiError) setError(e.problem.detail ?? e.problem.title ?? 'Could not compare these versions.');
        else throw e;
      }
    }
    void run();
    return () => {
      cancelled = true;
    };
  }, [specId, base, head, auth.user]);

  if (error) {
    return (
      <div className="card card-pad">
        <div className="card-t">Could not compare</div>
        <p style={{ color: 'var(--fg-2)', margin: '8px 0 0' }}>{error}</p>
      </div>
    );
  }

  if (!diff) return <div className="card card-pad">Comparing…</div>;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div>
        <div className="row-meta" style={{ marginBottom: 6 }}>
          <Link to={`/specs/${specId}`} className="mono faint">Back to the document</Link>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <h1 className="h1">Comparing {diff.base.label} with {diff.head.label}</h1>
        </div>
        <DiffSummaryLine diff={diff} />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '200px minmax(0, 1fr)', gap: 20, alignItems: 'start' }}>
        <aside>
          <DiffJumpList diff={diff} />
        </aside>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, minWidth: 0 }}>
          <div style={{ display: 'flex' }}>
            <span className="spacer" />
            <DiffModeToggle mode={mode} onChange={setMode} />
          </div>
          <DiffView diff={diff} mode={mode} />
        </div>
      </div>
    </div>
  );
}
