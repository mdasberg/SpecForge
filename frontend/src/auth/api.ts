import type { User } from 'oidc-client-ts';

/** An RFC 9457 problem document, which is how every SpecForge API error is rendered. */
export interface Problem {
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  errors?: Record<string, string>;
}

/**
 * Thrown for any non-2xx response, carrying the parsed problem document so a screen can show what
 * the API actually said instead of a generic failure.
 */
export class ApiError extends Error {
  readonly problem: Problem;

  constructor(problem: Problem) {
    super(problem.detail ?? problem.title ?? `Request failed with ${problem.status}`);
    this.name = 'ApiError';
    this.problem = problem;
  }
}

/** Raised when the token is gone or rejected, so the caller sends the user back through Keycloak. */
export class NotAuthenticatedError extends Error {
  constructor() {
    super('The session is no longer valid.');
    this.name = 'NotAuthenticatedError';
  }
}

/**
 * The single way the app talks to the API: it attaches the access token, turns problem+json into a
 * typed error, and reports a 401 as something the caller must resolve by re-authenticating rather
 * than by showing a dead end.
 */
export async function apiFetch<T>(path: string, user: User | null | undefined, init: RequestInit = {}): Promise<T> {
  if (!user || user.expired) {
    throw new NotAuthenticatedError();
  }

  const response = await fetch(path, {
    ...init,
    headers: {
      ...init.headers,
      Authorization: `Bearer ${user.access_token}`,
      Accept: 'application/json, application/problem+json',
    },
  });

  if (response.status === 401) {
    throw new NotAuthenticatedError();
  }

  if (!response.ok) {
    throw new ApiError(await readProblem(response));
  }

  return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
}

/**
 * A failing response is not guaranteed to be problem+json — a proxy or a crash can return HTML —
 * so the status is the one field always trusted.
 */
async function readProblem(response: Response): Promise<Problem> {
  try {
    const body = (await response.json()) as Partial<Problem>;
    return { ...body, status: response.status };
  } catch {
    return { status: response.status, title: response.statusText };
  }
}

/**
 * The `Identity` schema from `src/main/resources/openapi/specforge-api.yaml`, kept by hand. The
 * backend generates its types from that contract; the frontend does not yet, so this is the one
 * place the contract is duplicated rather than generated.
 */
export interface Identity {
  id: string;
  displayName: string;
  avatarUrl: string | null;
  actorKind: 'HUMAN' | 'AGENT';
  roles: Array<'REVIEWER' | 'ARCHITECT' | 'ADMIN'>;
}
