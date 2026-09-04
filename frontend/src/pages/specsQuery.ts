import type { ListSpecsParams, SpecGrouping, SpecStatus } from '../api/catalog';
import { SPEC_STATUSES } from '../lib/specStatus';

/** The spec browser's whole view state: grouping, active filters and the search term. */
export interface SpecsView {
  groupBy: SpecGrouping;
  status: SpecStatus[];
  owner: string[];
  team: string[];
  domain: string[];
  tag: string[];
  q: string;
}

type FilterKey = 'status' | 'owner' | 'team' | 'domain' | 'tag';

function parseGroupBy(value: string | null): SpecGrouping {
  return value === 'REPOSITORY' || value === 'DOMAIN' || value === 'TEAM' ? value : 'PROJECT';
}

function parseStatuses(values: string[]): SpecStatus[] {
  return values.filter((v): v is SpecStatus => (SPEC_STATUSES as string[]).includes(v));
}

function toggled(values: string[], value: string): string[] {
  return values.includes(value) ? values.filter((v) => v !== value) : [...values, value];
}

/** Reads the URL's query params into the view state the spec browser renders from. */
export function parseSpecsView(params: URLSearchParams): SpecsView {
  return {
    groupBy: parseGroupBy(params.get('groupBy')),
    status: parseStatuses(params.getAll('status')),
    owner: params.getAll('owner'),
    team: params.getAll('team'),
    domain: params.getAll('domain'),
    tag: params.getAll('tag'),
    q: params.get('q') ?? '',
  };
}

/** The inverse of `parseSpecsView` — what a shared URL for this view looks like. */
export function specsViewToParams(view: SpecsView): URLSearchParams {
  const params = new URLSearchParams();
  params.set('groupBy', view.groupBy);
  for (const value of view.status) params.append('status', value);
  for (const value of view.owner) params.append('owner', value);
  for (const value of view.team) params.append('team', value);
  for (const value of view.domain) params.append('domain', value);
  for (const value of view.tag) params.append('tag', value);
  if (view.q) params.set('q', view.q);
  return params;
}

/** Adds `value` to `key` if absent, removes it if present; every other param is untouched. */
export function toggleFilterValue(params: URLSearchParams, key: FilterKey, value: string): URLSearchParams {
  const next = new URLSearchParams(params);
  const current = next.getAll(key);
  next.delete(key);
  for (const v of toggled(current, value)) next.append(key, v);
  return next;
}

export function withGroupBy(params: URLSearchParams, value: SpecGrouping): URLSearchParams {
  const next = new URLSearchParams(params);
  next.set('groupBy', value);
  return next;
}

export function withSearchTerm(params: URLSearchParams, term: string): URLSearchParams {
  const next = new URLSearchParams(params);
  if (term) next.set('q', term);
  else next.delete('q');
  return next;
}

export function hasActiveFilters(view: SpecsView): boolean {
  return (
    Boolean(view.q) ||
    view.status.length > 0 ||
    view.owner.length > 0 ||
    view.team.length > 0 ||
    view.domain.length > 0 ||
    view.tag.length > 0
  );
}

/** What a view state translates to for `listSpecs`. */
export function viewToListParams(view: SpecsView, limit: number, cursor?: string): ListSpecsParams {
  return {
    groupBy: view.groupBy,
    status: view.status,
    owner: view.owner,
    team: view.team,
    domain: view.domain,
    tag: view.tag,
    q: view.q || undefined,
    limit,
    cursor,
  };
}
