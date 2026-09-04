import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router';
import { useAuth } from 'react-oidc-context';
import { getSpec } from '../api/catalog';
import type { SpecDetail } from '../api/catalog';
import { ApiError } from '../auth/api';
import { Avatar } from '../components/Avatar';
import { SpecMarkdown } from '../components/SpecMarkdown';
import { StatusBadge } from '../components/StatusBadge';
import { formatRelativeTime } from '../lib/format';

export function SpecDocument() {
  const { specId } = useParams<{ specId: string }>();
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const versionParam = searchParams.get('version');

  const [detail, setDetail] = useState<SpecDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!specId) return;
    let cancelled = false;
    async function run() {
      setLoading(true);
      setError(null);
      try {
        const result = await getSpec(specId as string, versionParam ? Number(versionParam) : undefined, auth.user);
        if (cancelled) return;
        setDetail(result);
      } catch (e) {
        if (cancelled) return;
        if (e instanceof ApiError) {
          setError(e.problem.detail ?? e.problem.title ?? 'Could not load this specification.');
        } else {
          throw e;
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    void run();
    return () => {
      cancelled = true;
    };
  }, [specId, versionParam, auth.user]);

  // Scroll to, and briefly highlight, the section named by the URL hash — on first load and on
  // every outline click (a Link to `#anchor` updates location.hash, which re-runs this).
  useEffect(() => {
    if (!detail || !location.hash) return;
    const id = decodeURIComponent(location.hash.slice(1));
    const el = document.getElementById(id);
    if (!el) return;
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    el.classList.add('hl-sel');
    const timer = setTimeout(() => el.classList.remove('hl-sel'), 2000);
    return () => clearTimeout(timer);
  }, [detail, location.hash]);

  // The anchors come from the backend, which parsed the document; deriving them again here would
  // be a second implementation of the same rule, free to disagree with the one discussions and
  // diffs address.
  const outlineAnchors = useMemo(
    () => (detail ? detail.version.sections.map((section) => section.anchorKey) : []),
    [detail],
  );

  // Default the comparison to "the previous version against the current one", the question a reader
  // asks first. Adjusted during render rather than in an effect, per React's guidance for resetting
  // state when the data it derives from changes.
  const [compareBase, setCompareBase] = useState(1);
  const [compareHead, setCompareHead] = useState(1);
  const [lastLoadedVersion, setLastLoadedVersion] = useState<string | null>(null);
  const loadedKey = detail ? `${detail.id}:${detail.versions.length}` : null;
  if (loadedKey && loadedKey !== lastLoadedVersion && detail) {
    const latest = detail.versions[detail.versions.length - 1].ordinal;
    setLastLoadedVersion(loadedKey);
    setCompareBase(detail.versions.length > 1 ? detail.versions[detail.versions.length - 2].ordinal : latest);
    setCompareHead(latest);
  }

  function selectVersion(ordinal: number) {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('version', String(ordinal));
      return next;
    });
  }

  if (loading && !detail) {
    return <div className="card card-pad">Loading specification…</div>;
  }

  if (error) {
    return (
      <div className="card card-pad">
        <div className="card-t">Could not load specification</div>
        <p style={{ color: 'var(--fg-2)', margin: '8px 0 0' }}>{error}</p>
      </div>
    );
  }

  if (!detail) return null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div>
        <div className="row-meta" style={{ marginBottom: 6 }}>
          <span>{detail.project}</span>
          <span className="sep">/</span>
          <span className="mono faint">{detail.repositoryFullName}</span>
          <span className="sep">/</span>
          <span className="mono faint">{detail.path}</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
          <h1 className="h1">{detail.title}</h1>
          <StatusBadge status={detail.status} />
          <span className="tag">v{detail.version.ordinal}</span>
          {!detail.version.current && <span className="badge b-changes">Older version</span>}
          {loading && <span className="faint">loading…</span>}
        </div>
        <div className="row-meta" style={{ marginTop: 5, flexWrap: 'wrap' }}>
          {detail.owner && (
            <>
              <span>Owner</span>
              <Avatar name={detail.owner} />
              <span style={{ color: 'var(--fg-2)' }}>{detail.owner}</span>
              <span className="sep">·</span>
            </>
          )}
          {detail.owningTeam && (
            <>
              <span>{detail.owningTeam}</span>
              <span className="sep">·</span>
            </>
          )}
          {detail.domain && (
            <>
              <span>{detail.domain}</span>
              <span className="sep">·</span>
            </>
          )}
          <span>updated {formatRelativeTime(detail.updatedAt)}</span>
          {detail.tags.length > 0 && (
            <>
              <span className="sep">·</span>
              <span style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                {detail.tags.map((tag) => (
                  <span key={tag} className="tag">{tag}</span>
                ))}
              </span>
            </>
          )}
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '200px minmax(0, 1fr)', gap: 20, alignItems: 'start' }}>
        <aside style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {detail.version.sections.length > 0 && (
            <div>
              <div className="side-t">On this page</div>
              <div className="side-group">
                {detail.version.sections.map((section, i) => {
                  const anchor = outlineAnchors[i];
                  const isActive = location.hash === `#${anchor}`;
                  return (
                    <Link
                      key={anchor}
                      to={{ search: location.search, hash: `#${anchor}` }}
                      className={`side-item sub${isActive ? ' on' : ''}`}
                      style={{ paddingLeft: 8 + (section.level - 1) * 12 }}
                    >
                      {section.heading}
                    </Link>
                  );
                })}
              </div>
            </div>
          )}

          <div>
            <div className="side-t">Versions</div>
            <div className="side-group">
              {detail.versions.map((v) => (
                <button
                  key={v.ordinal}
                  type="button"
                  className={`side-item${v.ordinal === detail.version.ordinal ? ' on' : ''}`}
                  style={{ width: '100%', textAlign: 'left', background: 'none', border: 'none', font: 'inherit', flexDirection: 'column', alignItems: 'flex-start', gap: 1 }}
                  onClick={() => selectVersion(v.ordinal)}
                >
                  <span>
                    v{v.ordinal} <span className="faint">{v.current ? '· current' : '· older version'}</span>
                  </span>
                  <span className="faint" style={{ fontSize: 11 }}>
                    {v.author ?? 'unknown author'} · {formatRelativeTime(v.createdAt)}
                  </span>
                </button>
              ))}
            </div>
          </div>

          {detail.versions.length > 1 && (
            <div>
              <div className="side-t">Compare</div>
              {/* Comparing two versions creates no review and no history — it only renders. The
                  affordance lives beside the version selector because that is where a reader
                  already is when they wonder what changed. */}
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '0 8px' }}>
                <select
                  className="input"
                  aria-label="Compare from version"
                  value={compareBase}
                  onChange={(e) => setCompareBase(Number(e.target.value))}
                  style={{ flex: '1 1 0', minWidth: 0 }}
                >
                  {detail.versions.map((v) => (
                    <option key={v.ordinal} value={v.ordinal}>v{v.ordinal}</option>
                  ))}
                </select>
                <span className="faint">→</span>
                <select
                  className="input"
                  aria-label="Compare to version"
                  value={compareHead}
                  onChange={(e) => setCompareHead(Number(e.target.value))}
                  style={{ flex: '1 1 0', minWidth: 0 }}
                >
                  {detail.versions.map((v) => (
                    <option key={v.ordinal} value={v.ordinal}>v{v.ordinal}</option>
                  ))}
                </select>
              </div>
              <button
                type="button"
                className="btn btn-sm"
                style={{ margin: '6px 8px 0' }}
                disabled={compareBase === compareHead}
                onClick={() => navigate(`/specs/${detail.id}/compare?base=${compareBase}&head=${compareHead}`)}
              >
                Compare
              </button>
            </div>
          )}
        </aside>

        <SpecMarkdown content={detail.version.content} anchors={outlineAnchors} />
      </div>
    </div>
  );
}
