# ScamTrace — Digital Risk Protection Platform (DRP)
### Automated Threat Intelligence, Social Media Impersonation & Rogue App Monitoring
**ByteXL Hackathon Platform Project**

---

## 📌 Overview

**ScamTrace** is an enterprise-grade **Digital Risk Protection Platform (DRP)** built to detect, analyze, and mitigate cyber threats, brand impersonation, rogue mobile applications, and phishing domains across the open web and social ecosystems.

Designed with strict multi-tenant isolation, cryptographically anchored asset ownership, and high-precision threat detection, ScamTrace empowers security operation centers (SOC) and brand protection teams to monitor digital footprints in real-time.

---

## 🏗️ Architecture & Technology Stack

The platform is designed around a decoupled, enterprise architecture:

| Component | Technology | Description |
|---|---|---|
| **Backend Core** | **Java 21 / Spring Boot 3.3.4** | High-throughput REST API with Spring Data JPA & Hibernate |
| **Security Layer** | **Spring Security 6 + JWT** | Stateless RBAC authentication & strict tenant isolation |
| **Database** | **PostgreSQL 16** | Relational data persistence with ACID guarantees |
| **Migrations** | **Flyway 10** | Deterministic, reproducible database schema migrations |
| **Frontend UI** | **React 18 + TypeScript + Vite** | Enterprise SaaS application shell with dark theme |
| **Styling & Icons** | **Modern Vanilla CSS / Lucide React** | Fluid design system with glassmorphic cards and badges |

---

## 🚀 Key Features Implemented

### 1. Multi-Tenant Architecture & Workspace Management
- **Organization Isolation:** Every user belongs to one or more organizations with granular roles (OWNER, ADMIN, ANALYST, VIEWER).
- **Context Switching:** Secure token exchange allowing authorized users to switch between organizations without re-authenticating.
- **Strict Boundary Guardrails:** TenantSecurityService dynamically validates that data requests match the caller's active organization JWT context.

### 2. Enterprise Authentication & Security
- **Stateless JWT Tokens:** Fast, secure token-based authentication signed via HMAC-SHA256.
- **Password Security:** Multi-round BCrypt encryption (strength 12).
- **Comprehensive Audit Trail:** Dedicated security_audit_logs tracking logins, registrations, tenant switches, membership changes, and client IP addresses.

### 3. Brand Profile & Official Asset Registry
- **Brand Identity Management:** Organization-scoped brand profiles supporting multiple brands per tenant.
- **Multi-Type Official Assets:**
  - **Domains / Websites:** Apex domain normalization, FQDN validation, and DNS TXT verification records.
  - **Social Media Accounts:** Official accounts across X (Twitter), LinkedIn, Facebook, Instagram, YouTube, and TikTok.
  - **Mobile Applications:** Verified store listings across Google Play (Package IDs) and Apple App Store (Bundle IDs).
- **Anti-Impersonation Engine Invariant:** Display-name similarity alone **never** establishes official ownership or excludes candidates from threat detection.

---

## 📂 Repository Structure

`
Scam-Trace/
├── backend/
│   ├── src/main/java/com/kampusvc/digitalrisk/
│   │   ├── auth/            # JWT authentication, user registration & login
│   │   ├── common/          # Global exception handler & standardized API envelopes
│   │   ├── config/          # Web & security configurations, CORS policy
│   │   ├── health/          # System health check endpoints
│   │   ├── identity/        # Organization, membership, and security audit log domain
│   │   └── security/        # JWT filters, custom UserPrincipal, TenantSecurityService
│   ├── src/main/resources/
│   │   ├── db/migration/    # Flyway database migration scripts (V1, V2)
│   │   └── application.properties
│   ├── pom.xml              # Maven dependencies & build configuration
│   └── mvnw / mvnw.cmd      # Maven wrapper
│
├── frontend/
│   ├── src/
│   │   ├── components/      # UI primitives (Cards, Badges, Header, AppLayout)
│   │   ├── context/         # AuthContext with tenant switching and session restore
│   │   ├── pages/           # Login, Register, Status, and Architecture views
│   │   ├── services/        # Axios-based API client with automatic Bearer token injection
│   │   └── App.tsx          # Client-side routing and protected routes
│   ├── package.json         # React 18, Vite, TypeScript dependencies
│   └── vite.config.ts       # Vite build & local dev server proxy configuration
│
└── README.md                # Project documentation
`

---

## ⚡ Quickstart & Local Setup

### Prerequisites
- **JDK 21** or later
- **Node.js 18+** & **npm**
- **PostgreSQL 16** (or portable PostgreSQL)

---

### Step 1: Start PostgreSQL Database
Ensure a PostgreSQL database named digital_risk_db is running on port 5432:

`sql
CREATE DATABASE digital_risk_db;
`

---

### Step 2: Start the Backend (Spring Boot)
Open a terminal in the ackend directory:

`ash
cd backend
./mvnw clean spring-boot:run
`
*(On Windows: .\mvnw.cmd spring-boot:run)*

The backend server will start at **http://localhost:8080**.  
Flyway will automatically run baseline migrations (V1, V2) on startup.

#### Health Verification:
`ash
curl http://localhost:8080/api/v1/health
`

---

### Step 3: Start the Frontend (Vite + React)
Open a separate terminal in the rontend directory:

`ash
cd frontend
npm install
npm run dev
`

The frontend will launch at **http://localhost:5173**.

---

## 🔒 API Endpoints Overview

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | /api/v1/health | Public | System and database health status |
| POST | /api/v1/auth/register | Public | Register new user + provision initial organization |
| POST | /api/v1/auth/login | Public | Authenticate user & issue JWT |
| GET | /api/v1/auth/me | Authenticated | Fetch current user details & organization memberships |
| GET | /api/v1/organizations | Authenticated | List all organizations the user is a member of |
| POST | /api/v1/organizations | Authenticated | Create a new organization workspace |
| POST | /api/v1/organizations/{id}/switch | Member | Switch active tenant context & issue refreshed JWT |
| GET | /api/v1/organizations/{id}/members | Member | List organization members and roles |
| POST | /api/v1/organizations/{id}/members | Owner/Admin | Invite or add member to organization |

---

## 🧪 Testing & Verification

Run backend unit and integration tests (including multi-tenant boundary checks):
`ash
cd backend
./mvnw test
`

Run frontend smoke tests:
`ash
cd frontend
npm test
`

---

## 👥 Contributors & Authors
- **Praveen Kumar Nasina** ([@nasinapraveenkumar](https://github.com/nasinapraveenkumar)) — Core Architecture, Security, Multi-Tenant Backend & React Integration
- **Manoz Kumar Boggavarapu** ([@boggavarapu-manoz-kumar](https://github.com/boggavarapu-manoz-kumar)) — Platform Development & Threat Detection Integration
