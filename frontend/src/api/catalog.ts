import type { User } from 'oidc-client-ts';
import { apiFetch } from '../auth/api';

/**
 * The `SpecGrouping` / `SpecStatus` / `SpecSummary` / `SpecList` / `SpecDetail` / `ProjectList`
 * schemas from `src/main/resources/openapi/specforge-api.yaml`, kept by hand — the same
 * convention `Identity` in `auth/api.ts` follows, since the frontend does not yet generate types
 * from the contract.
 */
export type SpecGrouping = 'PROJECT' | 'REPOSITORY' | 'DOMAIN' | 'TEAM';

export type SpecStatus = 'DRAFT' | 'IN_REVIEW' | 'CHANGES_REQUESTED' | 'APPROVED' | 'IMPLEMENTED';

export interface SpecMatch {
  anchorKey: string;
  heading: string;
  snippet: string;
}

export interface SpecSummary {
  id: string;
  title: string;
  path: string;
  project: string;
  repositoryFullName: string;
  domain?: string | null;
  owningTeam?: string | null;
  owner?: string | null;
  status: SpecStatus;
  tags: string[];
  currentVersion: number;
  updatedAt: string;
  groupKey: string;
  match?: SpecMatch;
}

export interface SpecGroupCount {
  key: string;
  count: number;
}

export interface SpecList {
  items: SpecSummary[];
  groups: SpecGroupCount[];
  groupBy: SpecGrouping;
  total: number;
  cursor?: string | null;
}

export interface SpecSection {
  anchorKey: string;
  heading: string;
  level: number;
  ordinal: number;
}

export interface SpecVersion {
  ordinal: number;
  author?: string | null;
  commitSha?: string | null;
  createdAt: string;
  current: boolean;
  content: string;
  sections: SpecSection[];
}

export interface SpecVersionSummary {
  ordinal: number;
  author?: string | null;
  commitSha?: string | null;
  createdAt: string;
  current: boolean;
}

export interface SpecDetail {
  id: string;
  title: string;
  path: string;
  project: string;
  repositoryFullName: string;
  domain?: string | null;
  owningTeam?: string | null;
  owner?: string | null;
  status: SpecStatus;
  tags: string[];
  createdAt: string;
  updatedAt: string;
  version: SpecVersion;
  versions: SpecVersionSummary[];
}

export interface ProjectCounts {
  draft: number;
  inReview: number;
  changesRequested: number;
  approved: number;
  implemented: number;
}

export interface ProjectSummary {
  name: string;
  team?: string | null;
  repositories: string[];
  specCount: number;
  counts: ProjectCounts;
  openReviews: number;
}

export interface ProjectList {
  items: ProjectSummary[];
}

export interface ListSpecsParams {
  groupBy?: SpecGrouping;
  status?: SpecStatus[];
  owner?: string[];
  team?: string[];
  domain?: string[];
  tag?: string[];
  q?: string;
  limit?: number;
  cursor?: string;
}

/** Appends a repeatable query parameter once per value, per the `GET /api/specs` contract. */
function buildQuery(params: ListSpecsParams): string {
  const search = new URLSearchParams();
  if (params.groupBy) search.set('groupBy', params.groupBy);
  for (const value of params.status ?? []) search.append('status', value);
  for (const value of params.owner ?? []) search.append('owner', value);
  for (const value of params.team ?? []) search.append('team', value);
  for (const value of params.domain ?? []) search.append('domain', value);
  for (const value of params.tag ?? []) search.append('tag', value);
  if (params.q) search.set('q', params.q);
  if (params.limit !== undefined) search.set('limit', String(params.limit));
  if (params.cursor) search.set('cursor', params.cursor);
  const qs = search.toString();
  return qs ? `?${qs}` : '';
}

export function listSpecs(params: ListSpecsParams, user: User | null | undefined): Promise<SpecList> {
  return apiFetch<SpecList>(`/api/specs${buildQuery(params)}`, user);
}

export function getSpec(specId: string, version: number | undefined, user: User | null | undefined): Promise<SpecDetail> {
  const qs = version !== undefined ? `?version=${version}` : '';
  return apiFetch<SpecDetail>(`/api/specs/${encodeURIComponent(specId)}${qs}`, user);
}

export function listProjects(user: User | null | undefined): Promise<ProjectList> {
  return apiFetch<ProjectList>('/api/projects', user);
}
