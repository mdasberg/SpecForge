import type { User } from 'oidc-client-ts';
import { apiFetch } from '../auth/api';
import type { ActorRef, TextRange } from './discussion';

/**
 * The `CheckRun*`, `Finding*` and `ChecksSummary` schemas from
 * `src/main/resources/openapi/specforge-api.yaml`, kept by hand — the same convention `approval.ts`
 * and `discussion.ts` follow, since the frontend does not yet generate types from the contract.
 */
export type CheckRunState = 'QUEUED' | 'RUNNING' | 'PASSED' | 'FAILED' | 'SKIPPED';

export type CheckRunnerKind = 'DETERMINISTIC' | 'MODEL';

export type FindingSeverity = 'INFO' | 'WARNING' | 'ERROR';

export type FindingDispositionKind = 'ACCEPTED' | 'DISMISSED';

export interface FindingDisposition {
  kind: FindingDispositionKind;
  actor: ActorRef;
  decidedAt: string;
  threadId?: string | null;
}

export interface Finding {
  id: string;
  checkRunId: string;
  checkKey: string;
  sectionKey: string;
  range?: TextRange | null;
  severity: FindingSeverity;
  title: string;
  body: string;
  proposedText?: string | null;
  author: ActorRef;
  modelIdentity?: string | null;
  disposition?: FindingDisposition | null;
  createdAt: string;
}

export interface CheckRun {
  id: string;
  checkKey: string;
  displayName: string;
  blocking: boolean;
  runnerKind: CheckRunnerKind;
  state: CheckRunState;
  headLabel: string;
  stale: boolean;
  summary?: string | null;
  modelIdentity?: string | null;
  durationMs?: number | null;
  queuedAt: string;
  startedAt?: string | null;
  finishedAt?: string | null;
  findings: Finding[];
}

/** `ChecksSummary` is shared with the approval panel, which is the point of the counts being here. */
export interface ChecksSummary {
  configured: boolean;
  failed: boolean;
  failedCheckNames: string[];
  totalCount: number;
  passedCount: number;
  failedCount: number;
  pendingCount: number;
}

export interface CheckRunList {
  items: CheckRun[];
  summary: ChecksSummary;
}

export function listChecks(reviewId: string, user: User | null | undefined): Promise<CheckRunList> {
  return apiFetch<CheckRunList>(`/api/reviews/${encodeURIComponent(reviewId)}/checks`, user);
}

export function rerunChecks(reviewId: string, user: User | null | undefined): Promise<CheckRunList> {
  return apiFetch<CheckRunList>(`/api/reviews/${encodeURIComponent(reviewId)}/checks`, user, { method: 'POST' });
}

export function rerunCheck(checkRunId: string, user: User | null | undefined): Promise<CheckRunList> {
  return apiFetch<CheckRunList>(`/api/checks/${encodeURIComponent(checkRunId)}/rerun`, user, { method: 'POST' });
}

export function acceptFinding(findingId: string, user: User | null | undefined): Promise<Finding> {
  return apiFetch<Finding>(`/api/findings/${encodeURIComponent(findingId)}/accept`, user, { method: 'POST' });
}

export function dismissFinding(findingId: string, user: User | null | undefined): Promise<Finding> {
  return apiFetch<Finding>(`/api/findings/${encodeURIComponent(findingId)}/dismiss`, user, { method: 'POST' });
}

export function discussFinding(
  findingId: string,
  body: string,
  user: User | null | undefined,
): Promise<Finding> {
  return apiFetch<Finding>(`/api/findings/${encodeURIComponent(findingId)}/discussions`, user, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ body }),
  });
}

export function undoFindingDisposition(findingId: string, user: User | null | undefined): Promise<Finding> {
  return apiFetch<Finding>(`/api/findings/${encodeURIComponent(findingId)}/disposition`, user, { method: 'DELETE' });
}
