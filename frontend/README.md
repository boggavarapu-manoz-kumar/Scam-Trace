# Digital Risk Protection Platform — Frontend

React + TypeScript frontend client for the Digital Risk Protection Platform (Kampus.VC).

## Requirements

- **Node.js:** Node.js 20+ (verified with Node.js v25.0.0)
- **Package Manager:** npm 10+ (verified with npm 11.6.2)

## Tech Stack

- **Framework:** React 19 + TypeScript (Strict Type Checking)
- **Tooling:** Vite 8
- **Styling:** Tailwind CSS v4 (`@tailwindcss/vite`) + Design System tokens
- **Routing:** React Router v7 (`react-router-dom`)
- **Icons:** Lucide Icons (`lucide-react`)
- **API Client:** Custom type-safe API client (`src/services/apiClient.ts`) with Bearer token injection
- **State:** `AuthContext.tsx` providing user profile, active tenant workspace, and RBAC visibility

## Directory Structure

```
frontend/
├── public/                 # Static assets
├── src/
│   ├── components/
│   │   ├── auth/           # ProtectedRoute higher-order guard
│   │   ├── common/         # EmptyState, ErrorState, LoadingState primitives
│   │   ├── layout/         # AppLayout — sidebar shell, active tenant badge, logout
│   │   ├── shell/          # DemoBanner and other shell-level overlays
│   │   └── ui/             # Badge, Card, CardHeader, PageHeader design primitives
│   ├── config/             # env.ts centralized configuration
│   ├── context/            # AuthContext.tsx — authentication & organization context
│   ├── hooks/              # useHealthStatus — typed health status state machine
│   ├── pages/              # StatusPage, ArchitecturePage, NotFoundPage, LoginPage, RegisterPage
│   ├── services/           # apiClient.ts, authService.ts
│   ├── smoke.test.mjs      # Fast verification test suite (15 assertions)
│   ├── App.tsx             # Main router declaration with protected routes
│   ├── main.tsx            # React application entry point
│   ├── index.css           # Design tokens, variables & typography
│   └── vite-env.d.ts       # Vite environment types
├── .env.example            # Environment template
├── tsconfig.json           # Strict TypeScript configuration
├── vite.config.ts          # Vite configuration with Tailwind & backend proxy
└── package.json            # Scripts and dependencies
```

## Running & Testing

```bash
# Run unit & smoke tests
npm test

# Build production bundle with TypeScript typecheck
npm run build

# Start local development server
npm run dev
```
