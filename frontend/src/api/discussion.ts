import type { User } from 'oidc-client-ts';
import { apiFetch } from '../auth/api';

/**
 * The `Thread*`, `Comment*`, `Notification*`, `ActorRef` and `MemberList` schemas from
 * `src/main/resources/openapi/specforge-api.yaml`, kept by hand — the same convention `review.ts`
 * follows, since the frontend does not yet generate types from the contract.
 */
export type ActorKind = 'HUMAN' | 'AGENT';

export type AnchorState = 'CURRENT' | 'STALE' | 'ORPHANED';

export type NotificationType = 'MENTION' | 'REPLY';

export interface ActorRef {
  subjectId: string;
  displayName: string;
  handle?: string | null;
  avatarUrl?: string | null;
  actorKind: ActorKind;
}

export interface TextRange {
  startOffset: number;
  endOffset: number;
}

export interface Comment {
  id: string;
  threadId: string;
  author: ActorRef;
  body: string;
  mentions: string[];
  createdAt: string;
  editedAt?: string | null;
}

export interface Thread {
  id: string;
  reviewId: string;
  sectionKey: string;
  range?: TextRange | null;
  quotedText: string;
  anchorVersionLabel: string;
  anchorState: AnchorState;
  opener: ActorRef;
  blocking: boolean;
  resolved: boolean;
  resolvedBy?: ActorRef | null;
  resolvedAt?: string | null;
  reopenedBy?: ActorRef | null;
  reopenedAt?: string | null;
  comments: Comment[];
  createdAt: string;
  updatedAt: string;
}

export interface ThreadList {
  items: Thread[];
  unresolvedBlockingCount: number;
  unresolvedNonBlockingCount: number;
}

export interface Notification {
  id: string;
  type: NotificationType;
  reviewId: string;
  threadId: string;
  commentId: string;
  actor: ActorRef;
  readAt?: string | null;
  createdAt: string;
}

export interface NotificationList {
  items: Notification[];
  unreadCount: number;
}

export interface MemberList {
  items: ActorRef[];
}

export function listThreads(reviewId: string, user: User | null | undefined): Promise<ThreadList> {
  return apiFetch<ThreadList>(`/api/reviews/${encodeURIComponent(reviewId)}/threads`, user);
}

export function openThread(
  reviewId: string,
  request: { sectionKey: string; range?: TextRange; quotedText?: string; body: string },
  user: User | null | undefined,
): Promise<Thread> {
  return apiFetch<Thread>(`/api/reviews/${encodeURIComponent(reviewId)}/threads`, user, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
}

export function replyToThread(threadId: string, body: string, user: User | null | undefined): Promise<Thread> {
  return apiFetch<Thread>(`/api/threads/${encodeURIComponent(threadId)}/comments`, user, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ body }),
  });
}

export function editComment(
  threadId: string,
  commentId: string,
  body: string,
  user: User | null | undefined,
): Promise<Thread> {
  return apiFetch<Thread>(
    `/api/threads/${encodeURIComponent(threadId)}/comments/${encodeURIComponent(commentId)}`,
    user,
    { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ body }) },
  );
}

export function resolveThread(threadId: string, user: User | null | undefined): Promise<Thread> {
  return apiFetch<Thread>(`/api/threads/${encodeURIComponent(threadId)}/resolve`, user, { method: 'POST' });
}

export function reopenThread(threadId: string, user: User | null | undefined): Promise<Thread> {
  return apiFetch<Thread>(`/api/threads/${encodeURIComponent(threadId)}/reopen`, user, { method: 'POST' });
}

export function listNotifications(user: User | null | undefined): Promise<NotificationList> {
  return apiFetch<NotificationList>('/api/notifications', user);
}

export function markNotificationRead(notificationId: string, user: User | null | undefined): Promise<Notification> {
  return apiFetch<Notification>(`/api/notifications/${encodeURIComponent(notificationId)}/read`, user, {
    method: 'POST',
  });
}

export function searchMembers(query: string, user: User | null | undefined): Promise<MemberList> {
  return apiFetch<MemberList>(`/api/members?query=${encodeURIComponent(query)}`, user);
}
