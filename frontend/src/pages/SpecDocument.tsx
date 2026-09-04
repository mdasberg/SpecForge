import { useEffect, useMemo, useState } from 'react';
import type { ComponentPropsWithoutRef } from 'react';
import { Link, useLocation, useParams, useSearchParams } from 'react-router';
import { useAuth } from 'react-oidc-context';
import ReactMarkdown from 'react-markdown';
import type { Components, ExtraProps } from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import { getSpec } from '../api/catalog';
import type { SpecDetail } from '../api/catalog';
import { ApiError } from '../auth/api';
import { Avatar } from '../components/Avatar';
import { StatusBadge } from '../components/StatusBadge';
import { formatRelativeTime } from '../lib/format';

type HeadingTag = 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';

export function SpecDocument() {
  const { specId } = useParams<{ specId: string }>();
  const auth = useAuth();
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

  function selectVersion(ordinal: number) {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('version', String(ordinal));
      return next;
    });
  }

  // Fresh per render: a heading component must not keep state across renders, or a re-render of
  // the same content would double-count and diverge from the outline. A `const`-bound counter
  // object (mutated via a property, never reassigned) is recreated on every call to this render
  // function, so there is nothing to go stale between renders.
  const headingCursor = { next: 0 };
  function nextHeadingId(): string {
    const id = outlineAnchors[headingCursor.next] ?? `section-${headingCursor.next + 1}`;
    headingCursor.next += 1;
    return id;
  }
  function heading(Tag: HeadingTag) {
    return function Heading({ children }: ComponentPropsWithoutRef<HeadingTag> & ExtraProps) {
      return <Tag id={nextHeadingId()}>{children}</Tag>;
    };
  }
  const markdownComponents: Components = {
    h1: heading('h1'),
    h2: heading('h2'),
    h3: heading('h3'),
    h4: heading('h4'),
    h5: heading('h5'),
    h6: heading('h6'),
  };

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
        </aside>

        <article className="doc" style={{ maxWidth: 'none' }}>
          <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeHighlight]} components={markdownComponents}>
            {detail.version.content}
          </ReactMarkdown>
        </article>
      </div>
    </div>
  );
}
