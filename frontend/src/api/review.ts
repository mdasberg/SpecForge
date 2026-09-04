import type { User } from 'oidc-client-ts';
import { apiFetch } from '../auth/api';
import type { SpecSection, SpecStatus } from './catalog';

/**
 * The `Review*`, `SpecDiff` and `Diff*` schemas from
 * `src/main/resources/openapi/specforge-api.yaml`, kept by hand — the same convention `catalog.ts`
 * follows, since the frontend does not yet generate types from the contract.
 */
export type ReviewState = 'OPEN' | 'CLOSED';

export type DiffChange = 'ADDED' | 'REMOVED' | 'MODIFIED' | 'UNCHANGED';

export type DiffLineType = 'CONTEXT' | 'ADDED' | 'REMOVED';

export interface SpecRef {
  id: string;
  title: string;
  path: string;
  project: string;
  repositoryFullName: string;
  status: SpecStatus;
}

export interface PullRequestRef {
  repositoryFullName: string;
  number: number;
  headSha: string;
}

export interface ReviewSide {
  ordinal?: number | null;
  label: string;
  contentSha: string;
  author?: string | null;
  commitSha?: string | null;
  createdAt?: string | null;
}

export interface ReviewSummary {
  id: string;
  spec: SpecRef;
  state: ReviewState;
  base: ReviewSide;
  head: ReviewSide;
  pullRequest?: PullRequestRef | null;
  openedBy?: string | null;
  openedAt: string;
  updatedAt: string;
}

export interface Review extends ReviewSummary {
  content: string;
  sections: SpecSection[];
}

export interface ReviewList {
  items: ReviewSummary[];
  total: number;
  cursor?: string | null;
}

export interface WordRange {
  start: number;
  end: number;
}

export interface DiffLine {
  type: DiffLineType;
  baseLine?: number | null;
  headLine?: number | null;
  text: string;
  words: WordRange[];
}

export interface DiffSection {
  anchorKey: string;
  heading: string;
  level: number;
  change: DiffChange;
  changedLines: number;
  author?: string | null;
  changedAt?: string | null;
  lines: DiffLine[];
}

export interface DiffSummary {
  addedSections: number;
  removedSections: number;
  modifiedSections: number;
  changedLines: number;
}

export interface SpecDiff {
  specId: string;
  base: ReviewSide;
  head: ReviewSide;
  summary: DiffSummary;
  sections: DiffSection[];
}

export function listReviews(
  params: { state?: ReviewState; specId?: string; limit?: number; cursor?: string },
  user: User | null | undefined,
): Promise<ReviewList> {
  const search = new URLSearchParams();
  if (params.state) search.set('state', params.state);
  if (params.specId) search.set('specId', params.specId);
  if (params.limit !== undefined) search.set('limit', String(params.limit));
  if (params.cursor) search.set('cursor', params.cursor);
  const qs = search.toString();
  return apiFetch<ReviewList>(`/api/reviews${qs ? `?${qs}` : ''}`, user);
}

export function getReview(reviewId: string, user: User | null | undefined): Promise<Review> {
  return apiFetch<Review>(`/api/reviews/${encodeURIComponent(reviewId)}`, user);
}

export function getReviewDiff(reviewId: string, user: User | null | undefined): Promise<SpecDiff> {
  return apiFetch<SpecDiff>(`/api/reviews/${encodeURIComponent(reviewId)}/diff`, user);
}

/** Comparing two versions creates nothing; it is the read-only counterpart of a review's diff. */
export function compareVersions(
  specId: string,
  base: number,
  head: number,
  user: User | null | undefined,
): Promise<SpecDiff> {
  return apiFetch<SpecDiff>(`/api/specs/${encodeURIComponent(specId)}/diff?base=${base}&head=${head}`, user);
}
