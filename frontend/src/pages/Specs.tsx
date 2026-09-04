import { useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { useAuth } from 'react-oidc-context';
import { listSpecs } from '../api/catalog';
import type { SpecGrouping, SpecGroupCount, SpecList, SpecStatus, SpecSummary } from '../api/catalog';
import { ApiError } from '../auth/api';
import { EmptyState } from '../components/EmptyState';
import { StatusBadge } from '../components/StatusBadge';
import { Avatar } from '../components/Avatar';
import { Button } from '../components/Button';
import { formatRelativeTime } from '../lib/format';
import { SPEC_STATUSES, specStatusLabel } from '../lib/specStatus';
import { hasActiveFilters, parseSpecsView, toggleFilterValue, viewToListParams, withGroupBy, withSearchTerm } from './specsQuery';

const GROUPINGS: { value: SpecGrouping; label: string }[] = [
  { value: 'PROJECT', label: 'Project' },
  { value: 'REPOSITORY', label: 'Repository' },
  { value: 'DOMAIN', label: 'Domain' },
  { value: 'TEAM', label: 'Team' },
];

/** Splits a `[[…]]`-marked snippet into plain text and highlighted `<mark>` runs. */
function Snippet({ text }: { text: string }) {
  const parts = text.split(/(\[\[|]])/);
  let marking = false;
  const nodes: ReactNode[] = [];
  parts.forEach((part, i) => {
    if (part === '[[') {
      marking = true;
      return;
    }
    if (part === ']]') {
      marking = false;
      return;
    }
    if (!part) return;
    nodes.push(marking ? <mark key={i} className="hl-cmt">{part}</mark> : <span key={i}>{part}</span>);
  });
  return <>{nodes}</>;
}

function ChipRow({ label, options, active, onToggle }: { label: string; options: string[]; active: string[]; onToggle: (value: string) => void }) {
  if (options.length === 0) return null;
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
      <span className="faint" style={{ fontSize: 11, minWidth: 44 }}>{label}</span>
      {options.map((opt) => (
        <button key={opt} type="button" className={`chip${active.includes(opt) ? ' on' : ''}`} onClick={() => onToggle(opt)}>
          {opt}
        </button>
      ))}
    </div>
  );
}

function SpecRow({ spec, onOpen }: { spec: SpecSummary; onOpen: () => void }) {
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
      style={{ flexDirection: 'column', alignItems: 'stretch', gap: 4 }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ flex: '1 1 auto', minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span className="row-t">{spec.title}</span>
            <span className="tag">v{spec.currentVersion}</span>
          </div>
          <div className="faint mono" style={{ fontSize: 11, marginTop: 1 }}>{spec.path}</div>
        </div>
        <StatusBadge status={spec.status} />
        {spec.owner && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 7, width: 132 }}>
            <Avatar name={spec.owner} />
            <span style={{ fontSize: 12 }}>{spec.owner}</span>
          </div>
        )}
        <span className="faint tnum" style={{ fontSize: 11.5, width: 64, textAlign: 'right' }}>{formatRelativeTime(spec.updatedAt)}</span>
      </div>
      {spec.match && (
        <div style={{ fontSize: 12, color: 'var(--fg-2)' }}>
          <span className="faint">{spec.match.heading}: </span>
          <Snippet text={spec.match.snippet} />
        </div>
      )}
    </div>
  );
}

function NoMatchState({ q, status, owner, team, domain, tag }: { q: string; status: string[]; owner: string[]; team: string[]; domain: string[]; tag: string[] }) {
  const parts: string[] = [];
  if (q) parts.push(`search "${q}"`);
  if (status.length > 0) parts.push(`status: ${status.map((s) => specStatusLabel(s as SpecStatus)).join(', ')}`);
  if (owner.length > 0) parts.push(`owner: ${owner.join(', ')}`);
  if (team.length > 0) parts.push(`team: ${team.join(', ')}`);
  if (domain.length > 0) parts.push(`domain: ${domain.join(', ')}`);
  if (tag.length > 0) parts.push(`tags: ${tag.join(', ')}`);
  return (
    <div className="card card-pad">
      <div className="card-t">No specifications match</div>
      <p style={{ color: 'var(--fg-2)', margin: '8px 0 0' }}>
        {parts.length > 0 ? `Nothing matches ${parts.join(' · ')}.` : 'No results.'}
      </p>
    </div>
  );
}

export function Specs() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const view = parseSpecsView(searchParams);
  const { groupBy, status: statusFilter, owner: ownerFilter, team: teamFilter, domain: domainFilter, tag: tagFilter, q } = view;

  const [searchInput, setSearchInput] = useState(q);
  const [list, setList] = useState<SpecList | null>(null);
  const [items, setItems] = useState<SpecSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // The debounced text box stays in sync when the URL changes from elsewhere (back/forward).
  // Adjusted during render rather than in an effect, per React's guidance for resetting state
  // when a prop (here, the URL's `q`) changes — avoids an extra post-paint render pass.
  const [lastUrlQ, setLastUrlQ] = useState(q);
  if (q !== lastUrlQ) {
    setLastUrlQ(q);
    setSearchInput(q);
  }

  // ~250ms after typing stops, push the term into the URL so it becomes the active filter.
  useEffect(() => {
    if (searchInput === q) return;
    const timer = setTimeout(() => {
      setSearchParams((prev) => withSearchTerm(prev, searchInput), { replace: true });
    }, 250);
    return () => clearTimeout(timer);
  }, [searchInput, q, setSearchParams]);

  const queryKey = searchParams.toString();

  // `view` is derived fresh from searchParams every render (new object identity each time), so
  // listing it as a dependency instead of queryKey would refetch on every render, not just on a
  // real filter change. queryKey (searchParams.toString()) is a stable string that changes
  // exactly when the view does, which is what "refetch on filter change" actually needs.
  /* oxlint-disable react-hooks/exhaustive-deps */
  useEffect(() => {
    let cancelled = false;
    async function run() {
      setLoading(true);
      setError(null);
      try {
        const result = await listSpecs(viewToListParams(view, 50), auth.user);
        if (cancelled) return;
        setList(result);
        setItems(result.items);
      } catch (e) {
        if (cancelled) return;
        if (e instanceof ApiError) {
          setError(e.problem.detail ?? e.problem.title ?? 'Could not load specifications.');
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
  }, [queryKey, auth.user]);
  /* oxlint-enable react-hooks/exhaustive-deps */

  async function loadMore() {
    if (!list?.cursor) return;
    setLoadingMore(true);
    try {
      const result = await listSpecs(viewToListParams(view, 50, list.cursor), auth.user);
      setList(result);
      setItems((prev) => [...prev, ...result.items]);
    } catch (e) {
      if (e instanceof ApiError) setError(e.problem.detail ?? e.problem.title ?? 'Could not load more specifications.');
      else throw e;
    } finally {
      setLoadingMore(false);
    }
  }

  function setGroupBy(value: SpecGrouping) {
    setSearchParams((prev) => withGroupBy(prev, value));
  }

  function toggleFilter(key: 'status' | 'owner' | 'team' | 'domain' | 'tag', value: string) {
    setSearchParams((prev) => toggleFilterValue(prev, key, value));
  }

  // ponytail: facet values are derived from the currently loaded page rather than a dedicated
  // facets endpoint (the API only returns group counts for the active groupBy). Upgrade to a real
  // facets endpoint if browsing needs to discover values outside the current result set.
  function facetValues(selector: (s: SpecSummary) => string | null | undefined, active: string[]): string[] {
    const values = new Set(active);
    for (const item of items) {
      const v = selector(item);
      if (v) values.add(v);
    }
    return [...values].sort();
  }

  const ownerOptions = facetValues((s) => s.owner, ownerFilter);
  const teamOptions = facetValues((s) => s.owningTeam, teamFilter);
  const domainOptions = facetValues((s) => s.domain, domainFilter);
  const tagValues = new Set(tagFilter);
  for (const item of items) for (const tag of item.tags) tagValues.add(tag);
  const tagOptions = [...tagValues].sort();

  const filtersActive = hasActiveFilters(view);

  const grouped = useMemo(() => {
    if (!list) return [];
    return list.groups
      .map((group: SpecGroupCount) => ({ group, rows: items.filter((item) => item.groupKey === group.key) }))
      .filter((bucket) => bucket.rows.length > 0);
  }, [list, items]);

  if (loading && !list) {
    return <div className="card card-pad">Loading specifications…</div>;
  }

  if (error) {
    return (
      <div className="card card-pad">
        <div className="card-t">Could not load specifications</div>
        <p style={{ color: 'var(--fg-2)', margin: '8px 0 0' }}>{error}</p>
      </div>
    );
  }

  if (!list) return null;

  if (list.total === 0 && !filtersActive) {
    return (
      <EmptyState title="No specifications yet">
        Connect a repository and SpecForge imports the specifications it holds, with a version per
        change.
      </EmptyState>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div style={{ display: 'flex', alignItems: 'flex-end', gap: 12 }}>
        <div>
          <h1 className="h1">Specifications</h1>
          <p className="sub">
            {list.total} specification{list.total === 1 ? '' : 's'}
            {loading && ' · updating…'}
          </p>
        </div>
        <div className="spacer" />
        <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: 'var(--fg-2)' }}>
          Group by
          <select className="input" value={groupBy} onChange={(e) => setGroupBy(e.target.value as SpecGrouping)}>
            {GROUPINGS.map((g) => (
              <option key={g.value} value={g.value}>{g.label}</option>
            ))}
          </select>
        </label>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
        <div className="searchbox" style={{ width: 280, background: 'var(--panel)' }}>
          <input
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search specifications…"
            aria-label="Search specifications"
            style={{ flex: '1 1 auto', border: 'none', outline: 'none', background: 'transparent', color: 'var(--fg)', font: 'inherit' }}
          />
        </div>
        {SPEC_STATUSES.map((status) => (
          <button
            key={status}
            type="button"
            className={`chip${statusFilter.includes(status) ? ' on' : ''}`}
            onClick={() => toggleFilter('status', status)}
          >
            {specStatusLabel(status)}
          </button>
        ))}
      </div>

      {(ownerOptions.length > 0 || teamOptions.length > 0 || domainOptions.length > 0 || tagOptions.length > 0) && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <ChipRow label="Owner" options={ownerOptions} active={ownerFilter} onToggle={(v) => toggleFilter('owner', v)} />
          <ChipRow label="Team" options={teamOptions} active={teamFilter} onToggle={(v) => toggleFilter('team', v)} />
          <ChipRow label="Domain" options={domainOptions} active={domainFilter} onToggle={(v) => toggleFilter('domain', v)} />
          <ChipRow label="Tags" options={tagOptions} active={tagFilter} onToggle={(v) => toggleFilter('tag', v)} />
        </div>
      )}

      {grouped.length === 0 ? (
        <NoMatchState q={q} status={statusFilter} owner={ownerFilter} team={teamFilter} domain={domainFilter} tag={tagFilter} />
      ) : (
        <div className="card">
          <div className="rows">
            {grouped.map(({ group, rows }) => (
              <div key={group.key}>
                <div className="row" style={{ background: 'var(--panel-2)', padding: '6px 14px' }}>
                  <b style={{ fontWeight: 600, fontSize: 12 }}>{group.key}</b>
                  <span className="faint" style={{ fontSize: 11 }}>{group.count} specification{group.count === 1 ? '' : 's'}</span>
                </div>
                {rows.map((row) => (
                  <SpecRow key={row.id} spec={row} onOpen={() => navigate(`/specs/${row.id}`)} />
                ))}
              </div>
            ))}
          </div>
        </div>
      )}

      {list.cursor && (
        <Button onClick={() => void loadMore()} disabled={loadingMore}>
          {loadingMore ? 'Loading…' : 'Load more'}
        </Button>
      )}
    </div>
  );
}
