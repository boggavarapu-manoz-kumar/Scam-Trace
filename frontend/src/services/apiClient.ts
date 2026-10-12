import { env } from '../config/env';

// ── Shared type contracts ────────────────────────────────────────────────

export interface ApiErrorPayload {
  code: string;
  message: string;
  details?: unknown;
}

export interface ApiResponseEnvelope<T> {
  success: boolean;
  message?: string;
  data?: T;
  error?: ApiErrorPayload;
  timestamp: string;
}

// ── Error class ──────────────────────────────────────────────────────────

export class ApiClientError extends Error {
  public readonly code: string;
  public readonly status: number;
  public readonly details?: unknown;

  constructor(message: string, code: string, status: number, details?: unknown) {
    super(message);
    this.name = 'ApiClientError';
    this.code = code;
    this.status = status;
    this.details = details;
  }
}

// ── In-Memory Token Management ───────────────────────────────────────────
// Follows approved Sprint 2.1 design: in-memory token state with fallback key for dev/demo

let inMemoryToken: string | null = null;
const STORAGE_KEY = 'scamtrace_auth_token';

// On boot, hydrate from sessionStorage/localStorage if available
try {
  inMemoryToken = sessionStorage.getItem(STORAGE_KEY) || localStorage.getItem(STORAGE_KEY);
} catch {
  // Storage access blocked or unavailable
}

export function setAuthToken(token: string | null): void {
  inMemoryToken = token;
  try {
    if (token) {
      sessionStorage.setItem(STORAGE_KEY, token);
      localStorage.setItem(STORAGE_KEY, token);
    } else {
      sessionStorage.removeItem(STORAGE_KEY);
      localStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    // Storage access blocked or unavailable
  }
}

export function getAuthToken(): string | null {
  return inMemoryToken;
}

// ── Request options ──────────────────────────────────────────────────────

export interface RequestOptions extends RequestInit {
  /** URL query parameters appended to the endpoint. */
  params?: Record<string, string | number | boolean | undefined>;
  /**
   * Request timeout in milliseconds.
   * Defaults to DEFAULT_TIMEOUT_MS. Pass 0 to disable.
   */
  timeoutMs?: number;
}

/** Default fetch timeout — prevents hanging requests. */
const DEFAULT_TIMEOUT_MS = 8_000;

// ── Core fetch wrapper ───────────────────────────────────────────────────

export async function apiClient<T>(
  endpoint: string,
  options: RequestOptions = {},
): Promise<T> {
  const { params, headers, timeoutMs = DEFAULT_TIMEOUT_MS, signal: callerSignal, ...rest } = options;

  // Build URL
  let url = `${env.apiBaseUrl.replace(/\/$/, '')}/${endpoint.replace(/^\//, '')}`;
  if (params) {
    const sp = new URLSearchParams();
    for (const [k, v] of Object.entries(params)) {
      if (v !== undefined) sp.append(k, String(v));
    }
    const qs = sp.toString();
    if (qs) url += `?${qs}`;
  }

  // Timeout via AbortController
  const controller = new AbortController();
  let timeoutId: ReturnType<typeof setTimeout> | undefined;
  if (timeoutMs > 0) {
    timeoutId = setTimeout(() => controller.abort('timeout'), timeoutMs);
  }

  // Merge caller signal with our timeout signal
  const combinedSignal: AbortSignal = callerSignal
    ? AbortSignal.any([controller.signal, callerSignal])
    : controller.signal;

  // Attach Authorization header if token exists
  const token = getAuthToken();
  const authHeaders: Record<string, string> = {};
  if (token) {
    authHeaders['Authorization'] = `Bearer ${token}`;
  }

  const config: RequestInit = {
    ...rest,
    signal: combinedSignal,
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...authHeaders,
      ...headers,
    },
  };

  let response: Response;
  try {
    response = await fetch(url, config);
  } catch (networkError) {
    if (
      networkError instanceof DOMException &&
      networkError.name === 'AbortError'
    ) {
      const isTimeout = (networkError as DOMException).message === 'timeout' ||
        controller.signal.aborted;
      throw new ApiClientError(
        isTimeout
          ? 'Request timed out. The backend did not respond in time.'
          : 'Request was cancelled.',
        isTimeout ? 'REQUEST_TIMEOUT' : 'REQUEST_CANCELLED',
        0,
      );
    }
    throw new ApiClientError(
      'Network connection failed. Unable to reach the backend service.',
      'NETWORK_FAILURE',
      0,
    );
  } finally {
    clearTimeout(timeoutId);
  }

  // Parse JSON body
  let json: ApiResponseEnvelope<T> | null = null;
  const ct = response.headers.get('content-type') ?? '';
  if (ct.includes('application/json')) {
    try {
      json = (await response.json()) as ApiResponseEnvelope<T>;
    } catch {
      // non-JSON body — fall through, handled below
    }
  }

  if (!response.ok) {
    // If 401 Unauthorized received and not during login/register, clear invalid token
    if (response.status === 401 && !endpoint.includes('auth/login') && !endpoint.includes('auth/register')) {
      setAuthToken(null);
    }

    const code = json?.error?.code ?? `HTTP_${response.status}`;
    const message =
      json?.error?.message ??
      json?.message ??
      'The server returned an unexpected error. Please try again.';
    throw new ApiClientError(message, code, response.status, json?.error?.details);
  }

  // Validate envelope shape
  if (json === null) {
    throw new ApiClientError(
      'Server returned a non-JSON response.',
      'INVALID_RESPONSE',
      response.status,
    );
  }

  if (json.data !== undefined) return json.data;
  return json as unknown as T;
}

// ── Health API ───────────────────────────────────────────────────────────

export interface AppHealth {
  status: string;
  environment: string;
  timestamp: string;
}

export const healthApi = {
  checkHealth: (signal?: AbortSignal): Promise<AppHealth> =>
    apiClient<AppHealth>('health', { signal }),
};
