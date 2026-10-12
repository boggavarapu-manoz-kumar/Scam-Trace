import React from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  ShieldCheck,
  Activity,
  Layers,
  AlertTriangle,
  Eye,
  Smartphone,
  BarChart2,
  Settings,
  LogOut,
  Building,
} from 'lucide-react';
import { DemoBanner } from '../shell/DemoBanner';
import { Badge } from '../ui/Badge';
import { useAuth } from '../../context/AuthContext';

/* ── Navigation schema ─────────────────────────────────────────────────── */
interface NavItem {
  to: string;
  end?: boolean;
  icon: React.ReactNode;
  label: string;
  soon?: boolean;
  minRole?: 'OWNER' | 'ADMIN' | 'ANALYST' | 'VIEWER';
}

interface NavSection {
  label: string;
  items: NavItem[];
}

const SOON_BADGE = (
  <Badge variant="amber" className="ml-auto" style={{ fontSize: '0.625rem', padding: '0.125rem 0.375rem' }}>
    Soon
  </Badge>
);

const NAV: NavSection[] = [
  {
    label: 'Overview',
    items: [
      { to: '/', end: true, icon: <Activity className="sidebar-link-icon" />, label: 'Platform Status' },
      { to: '/architecture', icon: <Layers className="sidebar-link-icon" />, label: 'Architecture' },
    ],
  },
  {
    label: 'Monitoring',
    items: [
      { to: '/threats', icon: <AlertTriangle className="sidebar-link-icon" />, label: 'Threat Detections', soon: true },
      { to: '/social', icon: <Eye className="sidebar-link-icon" />, label: 'Social Monitoring', soon: true },
      { to: '/apps', icon: <Smartphone className="sidebar-link-icon" />, label: 'App Store Monitoring', soon: true },
    ],
  },
  {
    label: 'Intelligence',
    items: [
      { to: '/analytics', icon: <BarChart2 className="sidebar-link-icon" />, label: 'Risk Analytics', soon: true },
    ],
  },
  {
    label: 'Administration',
    items: [
      { to: '/settings', icon: <Settings className="sidebar-link-icon" />, label: 'Settings & Members', soon: true, minRole: 'ADMIN' },
    ],
  },
];

/* ── Layout component ──────────────────────────────────────────────────── */
export const AppLayout: React.FC = () => {
  const { user, activeOrganization, activeRole, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  const getRoleBadgeVariant = (role: string | null) => {
    switch (role) {
      case 'OWNER':
        return 'amber';
      case 'ADMIN':
        return 'blue';
      case 'ANALYST':
        return 'green';
      default:
        return 'slate';
    }
  };

  return (
    <div className="app-shell">
      {/* ── Sidebar ── */}
      <aside className="sidebar" aria-label="Primary navigation">
        {/* Brand mark */}
        <div className="sidebar-brand">
          <div className="sidebar-brand-icon" aria-hidden="true">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <div>
            <div className="sidebar-brand-name">Digital Risk Protection</div>
            <div className="sidebar-brand-sub">Social & App Monitoring</div>
          </div>
        </div>

        {/* Tenant Organization Context Card */}
        {activeOrganization && (
          <div className="mx-3 my-2 p-2.5 rounded-lg bg-slate-800/80 border border-slate-700/60 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2 min-w-0">
              <Building className="h-4 w-4 text-blue-400 shrink-0" />
              <div className="min-w-0">
                <div className="font-semibold text-slate-200 truncate">{activeOrganization.name}</div>
                <div className="text-[10px] text-slate-400 truncate">Tenant Workspace</div>
              </div>
            </div>
            {activeRole && (
              <Badge variant={getRoleBadgeVariant(activeRole)} style={{ fontSize: '0.625rem', padding: '0.1rem 0.35rem' }}>
                {activeRole}
              </Badge>
            )}
          </div>
        )}

        {/* Navigation */}
        <nav className="sidebar-nav" aria-label="Main navigation">
          {NAV.map((section) => (
            <div key={section.label} className="mb-5">
              <div className="sidebar-section-label" aria-hidden="true">
                {section.label}
              </div>
              {section.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) =>
                    `sidebar-link${isActive ? ' active' : ''}`
                  }
                  aria-label={item.label}
                >
                  {item.icon}
                  <span className="flex-1 min-w-0 truncate">{item.label}</span>
                  {item.soon && SOON_BADGE}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        {/* Footer / user info & logout */}
        <div className="sidebar-footer">
          {user ? (
            <div className="flex items-center justify-between">
              <div className="min-w-0 pr-2">
                <div className="text-xs font-semibold text-slate-200 truncate">{user.fullName}</div>
                <div className="text-[10px] text-slate-400 truncate">{user.email}</div>
              </div>
              <button
                onClick={handleLogout}
                className="p-1.5 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-red-400 transition-colors"
                title="Sign out"
                aria-label="Sign out"
              >
                <LogOut className="h-4 w-4" />
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-1.5">
              <span
                className="h-2 w-2 rounded-full flex-shrink-0"
                style={{ background: '#10b981', boxShadow: '0 0 0 3px rgba(16,185,129,0.15)' }}
                aria-hidden="true"
              />
              <span style={{ fontSize: '0.6875rem', fontWeight: 500, color: 'var(--sidebar-text)' }}>
                REST API v1 Ready
              </span>
            </div>
          )}
        </div>
      </aside>

      {/* ── Main area ── */}
      <div className="main-content">
        {/* Hackathon demo notice */}
        <DemoBanner />

        {/* Topbar — context / environment indicators */}
        <header className="content-header flex items-center justify-between" aria-label="Topbar">
          <div className="flex items-center gap-2">
            <Badge variant="blue" dot>ByteXL Hackathon</Badge>
            <Badge variant="slate">v0.1.0-alpha</Badge>
          </div>
          {user && (
            <div className="text-xs text-slate-500 font-medium hidden sm:block">
              Authenticated Session: <span className="text-slate-700">{user.email}</span>
            </div>
          )}
        </header>

        {/* Routed page content */}
        <main className="page-body" id="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
