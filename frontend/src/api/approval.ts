import type { User } from 'oidc-client-ts';
import { apiFetch } from '../auth/api';
import type { ActorRef } from './discussion';

/**
 * The `Approval*`, `RequiredReviewer`, `Verdict*`, `ChecksSummary` and `ComposerState` schemas from
 * `src/main/resources/openapi/specforge-api.yaml`, kept by hand — the same convention `discussion.ts`
 * and `review.ts` follow, since the frontend does not yet generate types from the contract.
 */
export type VerdictType = 'APPROVE' | 'REQUEST_CHANGES' | 'COMMENT';

export type ReviewerOrigin = 'RULE' | 'MANUAL';

export type ReviewerState = 'PENDING' | 'APPROVED' | 'CHANGES_REQUESTED';

export interface RequiredReviewer {
  id: string;
  origin: ReviewerOrigin;
  requiredRole?: string | null;
  reviewer?: ActorRef | null;
  state: ReviewerState;
  addedBy?: ActorRef | null;
  addedAt?: string | null;
}

export interface ApprovalRuleState {
  satisfied: boolean;
  approvedCount: number;
  requiredCount: number;
  reason: string;
}

/** Re-exported from `agent.ts`: one schema, one type, so the panel and the Checks tab share it. */
export type { ChecksSummary } from './agent';

export interface ApprovalGateState {
  passed: boolean;
  reasons: string[];
}

export interface Verdict {
  id: string;
  author: ActorRef;
  verdictType: VerdictType;
  body?: string | null;
  headLabel: string;
  castAt: string;
}

export interface ComposerState {
  canApprove: boolean;
  canRequestChanges: boolean;
  canComment: boolean;
  disabledReason?: string | null;
}

export interface ApprovalStatus {
  reviewId: string;
  reviewers: RequiredReviewer[];
  rule: ApprovalRuleState;
  unresolvedBlockingThreads: number;
  checks: import('./agent').ChecksSummary;
  gate: ApprovalGateState;
  verdicts: Verdict[];
  composer: ComposerState;
}

export function getApprovalStatus(reviewId: string, user: User | null | undefined): Promise<ApprovalStatus> {
  return apiFetch<ApprovalStatus>(`/api/reviews/${encodeURIComponent(reviewId)}/approval`, user);
}

export function castVerdict(
  reviewId: string,
  request: { verdictType: VerdictType; body?: string; atHeadSha: string },
  user: User | null | undefined,
): Promise<ApprovalStatus> {
  return apiFetch<ApprovalStatus>(`/api/reviews/${encodeURIComponent(reviewId)}/verdicts`, user, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
}

export function addRequiredReviewer(
  reviewId: string,
  subjectId: string,
  user: User | null | undefined,
): Promise<ApprovalStatus> {
  return apiFetch<ApprovalStatus>(`/api/reviews/${encodeURIComponent(reviewId)}/required-reviewers`, user, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ subjectId }),
  });
}

export function removeRequiredReviewer(
  reviewId: string,
  requiredReviewerId: string,
  user: User | null | undefined,
): Promise<ApprovalStatus> {
  return apiFetch<ApprovalStatus>(
    `/api/reviews/${encodeURIComponent(reviewId)}/required-reviewers/${encodeURIComponent(requiredReviewerId)}`,
    user,
    { method: 'DELETE' },
  );
}
