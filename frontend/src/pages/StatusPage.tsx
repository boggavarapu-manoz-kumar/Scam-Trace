import React from 'react';
import {
  CheckCircle2,
  Server,
  Clock,
  RefreshCw,
  WifiOff,
  AlertTriangle,
  Loader2,
} from 'lucide-react';
import { useHealthStatus } from '../hooks/useHealthStatus';
import { PageHeader } from '../components/ui/PageHeader';
import { Badge } from '../components/ui/Badge';
import { Card, CardHeader } from '../components/ui/Card';

// ── Sprint roadmap rows ──────────────────────────────────────────────────

const SPRINT_ROWS: Array<{
  label: string;
  status: 'done' | 'active' | 'planned';
}> = [
  { label: 'Sprint 1.1 — Spring Boot 3.5.x & Java 21 Bootstrap',           status: 'done'    },
  { label: 'Sprint 1.2 — Modular Architecture & Centralized Error Handling', status: 'done'   },
  { label: 'Sprint 1.3 — PostgreSQL & Flyway Baseline Migrations',           status: 'done'   },
  { label: 'Sprint 1.4 — React + TypeScript + Tailwind Foundation',          status: 'done'   },
  { label: 'Sprint 1.5 — Professional Security SaaS Design System',          status: 'done'   },
  { label: 'Sprint 1.6 — Frontend / Backend Integration & CORS',             status: 'active' },
  { label: 'Sprint 2.x — Brand Monitoring & Threat Detection Modules',       status: 'planned'},
];

const VARIANT_MAP: Record<string, 'green' | 'blue' | 'slate'> = {
  done:    'green',
  active:  'blue',
  planned: 'slate',
};

const LABEL_MAP: Record<string, string> = {
  done:    'Completed',
  active:  'Active',
  planned: 'Planned',
};

// ── Connectivity indicator ───────────────────────────────────────────────

/** Compact status badge shown in the page header */
const ConnectivityBadge: React.FC<{ state: ReturnType<typeof useHealthStatus>['state'] }> = ({ state }) => {
  if (state.status === 'loading') return <Badge variant="slate" dot>Checking…</Badge>;
  if (state.status === 'success') return <Badge variant="green" dot>Backend Connected</Badge>;
  if (state.status === 'error')   return <Badge variant="red"   dot>Backend Unreachable</Badge>;
  return null;
};

// ── Stat cards ───────────────────────────────────────────────────────────

interface StatCardProps {
  label: string;
  value: React.ReactNode;
  meta: string;
  icon: React.ReactNode;
}

const StatCard: React.FC<StatCardProps> = ({ label, value, meta, icon }) => (
  <div className="stat-card">
    <div className="flex items-center justify-between">
      <p className="stat-label">{label}</p>
      {icon}
    </div>
    <div className="stat-value">{value}</div>
    <p className="stat-meta">{meta}</p>
  </div>
);

// ── Page component ───────────────────────────────────────────────────────

export const StatusPage: React.FC = () => {
  const { state, refetch } = useHealthStatus();

  const isLoading = state.status === 'loading';

  return (
    <div className="space-y-6">
      <PageHeader
        title="Platform Status"
        description="Live connectivity and architectural health of the Digital Risk Protection Platform."
        actions={
          <>
            <ConnectivityBadge state={state} />
            <button
              type="button"
              id="refresh-health-btn"
              onClick={refetch}
              disabled={isLoading}
              className="btn btn-secondary btn-sm"
              aria-label="Refresh backend connectivity"
            >
              <RefreshCw
                className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`}
                aria-hidden="true"
              />
              {isLoading ? 'Checking…' : 'Refresh'}
            </button>
          </>
        }
      />

      {/* ── Loading state ── */}
      {state.status === 'loading' && (
        <div
          className="stat-card flex items-center gap-3"
          style={{ color: 'var(--text-secondary)' }}
          role="status"
          aria-live="polite"
          aria-label="Connecting to backend"
        >
          <Loader2 className="h-5 w-5 animate-spin flex-shrink-0" style={{ color: 'var(--accent)' }} aria-hidden="true" />
          <span className="text-sm">Connecting to <code className="font-mono text-xs">/api/v1/health</code>…</span>
        </div>
      )}

      {/* ── Error state ── */}
      {state.status === 'error' && (
        <div
          className="rounded-lg border p-4 flex items-start gap-3"
          style={{
            borderColor: 'var(--red-border)',
            background: 'var(--red-bg)',
            color: 'var(--red)',
          }}
          role="alert"
          aria-live="assertive"
        >
          <AlertTriangle className="h-5 w-5 flex-shrink-0 mt-0.5" aria-hidden="true" />
          <div>
            <p className="text-sm font-semibold">Backend Service Unreachable</p>
            <p className="text-xs mt-1" style={{ color: 'var(--text-secondary)' }}>
              {/* Surface only the sanitized user-facing message, never raw internals */}
              {state.error.message}
              {state.error.code && (
                <span className="ml-1 opacity-70">({state.error.code})</span>
              )}
            </p>
            <p className="text-xs mt-2" style={{ color: 'var(--text-secondary)' }}>
              The platform interface remains available. Retry when the backend service is running.
            </p>
          </div>
        </div>
      )}

      {/* ── Idle state ── */}
      {state.status === 'idle' && (
        <div className="stat-card flex items-center gap-3" style={{ color: 'var(--text-secondary)' }}>
          <WifiOff className="h-5 w-5 flex-shrink-0" aria-hidden="true" />
          <span className="text-sm">Backend not yet contacted. Click <strong>Refresh</strong> to check connectivity.</span>
        </div>
      )}

      {/* ── Success state — three stat cards ── */}
      {state.status === 'success' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4" aria-live="polite">
          <StatCard
            label="API Status"
            value={state.data.status}
            meta="Reported via /api/v1/health"
            icon={<CheckCircle2 className="h-4 w-4" style={{ color: 'var(--green)' }} aria-hidden="true" />}
          />
          <StatCard
            label="Environment"
            value={<span className="capitalize">{state.data.environment}</span>}
            meta="Externalized Spring configuration"
            icon={<Server className="h-4 w-4" style={{ color: 'var(--blue)' }} aria-hidden="true" />}
          />
          <StatCard
            label="Backend Time (UTC)"
            value={
              <span className="text-base font-semibold break-all" style={{ fontSize: '0.9rem' }}>
                {state.data.timestamp}
              </span>
            }
            meta="ISO-8601 timestamp from backend"
            icon={<Clock className="h-4 w-4" style={{ color: 'var(--text-muted)' }} aria-hidden="true" />}
          />
        </div>
      )}

      {/* ── Sprint Roadmap table — always visible ── */}
      <Card padding={false}>
        <div className="p-5">
          <CardHeader
            title="Sprint Roadmap & Module Readiness"
            description="Foundation build status across backend and frontend sprints."
          />
        </div>
        <hr className="divider" />
        <table className="data-table" aria-label="Sprint roadmap">
          <thead>
            <tr>
              <th>Sprint</th>
              <th style={{ width: '8rem' }}>Status</th>
            </tr>
          </thead>
          <tbody>
            {SPRINT_ROWS.map((row) => (
              <tr key={row.label}>
                <td className="font-medium">{row.label}</td>
                <td>
                  <Badge variant={VARIANT_MAP[row.status]} dot>
                    {LABEL_MAP[row.status]}
                  </Badge>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
};
