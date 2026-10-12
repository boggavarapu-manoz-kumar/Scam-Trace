import { useCallback, useEffect, useReducer, useRef } from 'react';
import { AppHealth, ApiClientError, healthApi } from '../services/apiClient';

// ── State machine ────────────────────────────────────────────────────────

export type HealthState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: AppHealth; fetchedAt: number }
  | { status: 'error'; error: ApiClientError; fetchedAt: number };

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; data: AppHealth }
  | { type: 'FETCH_ERROR'; error: ApiClientError };

function reducer(_: HealthState, action: Action): HealthState {
  switch (action.type) {
    case 'FETCH_START':
      return { status: 'loading' };
    case 'FETCH_SUCCESS':
      return { status: 'success', data: action.data, fetchedAt: Date.now() };
    case 'FETCH_ERROR':
      return { status: 'error', error: action.error, fetchedAt: Date.now() };
  }
}

// ── Hook ─────────────────────────────────────────────────────────────────

export interface UseHealthStatusResult {
  state: HealthState;
  /** Trigger a fresh health check. Safe to call multiple times — concurrent
   *  in-flight requests are cancelled before a new one starts. */
  refetch: () => void;
}

/**
 * Fetches backend health status exactly once on mount, then only when
 * {@code refetch} is explicitly called. There is no automatic polling or
 * retry loop — keeping requests minimal and predictable.
 */
export function useHealthStatus(): UseHealthStatusResult {
  const [state, dispatch] = useReducer(reducer, { status: 'idle' });

  // Ref to the AbortController for the current in-flight request
  const abortRef = useRef<AbortController | null>(null);

  const fetchHealth = useCallback(() => {
    // Cancel any in-flight request before starting a new one
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    dispatch({ type: 'FETCH_START' });

    healthApi
      .checkHealth(controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          dispatch({ type: 'FETCH_SUCCESS', data });
        }
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return; // cancelled — discard
        const apiError =
          err instanceof ApiClientError
            ? err
            : new ApiClientError(
                'An unexpected error occurred while checking backend health.',
                'UNKNOWN',
                0,
              );
        // REQUEST_CANCELLED is a user-initiated abort — not an error to surface
        if (apiError.code === 'REQUEST_CANCELLED') return;
        dispatch({ type: 'FETCH_ERROR', error: apiError });
      });
  }, []);

  useEffect(() => {
    fetchHealth();
    return () => {
      // Abort on unmount to prevent state updates on an unmounted component
      abortRef.current?.abort();
    };
  }, [fetchHealth]);

  return { state, refetch: fetchHealth };
}
