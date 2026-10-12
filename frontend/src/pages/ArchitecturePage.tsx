import React from 'react';
import { Shield, Cpu, Database, Globe } from 'lucide-react';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardHeader } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';

interface StackItem { label: string; value: string }

const BACKEND_STACK: StackItem[] = [
  { label: 'Runtime',         value: 'Java 21 (LTS)' },
  { label: 'Framework',       value: 'Spring Boot 3.5.x' },
  { label: 'REST API',        value: 'Versioned prefix /api/v1' },
  { label: 'Persistence',     value: 'Spring Data JPA + Hibernate' },
  { label: 'Database',        value: 'PostgreSQL' },
  { label: 'Schema Mgmt',     value: 'Flyway migrations' },
  { label: 'Validation',      value: 'Bean Validation (Jakarta)' },
  { label: 'Observability',   value: 'Spring Boot Actuator' },
  { label: 'Testing',         value: 'JUnit 5, Mockito, Spring Test' },
  { label: 'Build',           value: 'Apache Maven' },
];

const FRONTEND_STACK: StackItem[] = [
  { label: 'Framework',   value: 'React 19 + TypeScript (strict)' },
  { label: 'Bundler',     value: 'Vite 8' },
  { label: 'Styling',     value: 'Tailwind CSS v4' },
  { label: 'Routing',     value: 'React Router 7 (declarative)' },
  { label: 'API Client',  value: 'Custom type-safe fetch wrapper' },
  { label: 'Icons',       value: 'Lucide React' },
];

const StackTable: React.FC<{ items: StackItem[] }> = ({ items }) => (
  <table className="data-table" aria-label="Stack details">
    <thead>
      <tr>
        <th style={{ width: '8rem' }}>Layer</th>
        <th>Technology</th>
      </tr>
    </thead>
    <tbody>
      {items.map(({ label, value }) => (
        <tr key={label}>
          <td className="font-medium" style={{ color: 'var(--text-secondary)', fontSize: '0.75rem' }}>{label}</td>
          <td style={{ fontWeight: 500 }}>{value}</td>
        </tr>
      ))}
    </tbody>
  </table>
);

const ARCH_LAYERS = [
  { icon: <Globe  className="h-4 w-4" />, title: 'API Layer',          desc: 'Versioned REST controllers with Bean Validation and centralized exception handling via @RestControllerAdvice.' },
  { icon: <Cpu    className="h-4 w-4" />, title: 'Service Layer',      desc: 'Business logic and orchestration. Pure Spring services with explicit transaction boundaries.' },
  { icon: <Shield className="h-4 w-4" />, title: 'Security Layer',     desc: 'Spring Security to be introduced in a dedicated sprint. No ad-hoc security scattered across layers.' },
  { icon: <Database className="h-4 w-4"/>, title: 'Persistence Layer', desc: 'Spring Data JPA repositories. Flyway owns all schema evolution — no auto DDL in production.' },
];

export const ArchitecturePage: React.FC = () => (
  <div className="space-y-6">
    <PageHeader
      title="System Architecture"
      description="Modular monolith with strict separation of concerns and versioned REST APIs."
      actions={<Badge variant="blue">Modular Monolith</Badge>}
    />

    {/* Stack tables */}
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <Card padding={false}>
        <div className="p-5">
          <CardHeader
            title="Backend Technology Stack"
            description="Java 21 · Spring Boot 3.5.x · PostgreSQL · Maven"
            action={<Cpu className="h-4 w-4" style={{ color: 'var(--blue)' }} aria-hidden="true" />}
          />
        </div>
        <hr className="divider" />
        <StackTable items={BACKEND_STACK} />
      </Card>

      <Card padding={false}>
        <div className="p-5">
          <CardHeader
            title="Frontend Technology Stack"
            description="React 19 · TypeScript · Vite · Tailwind CSS v4"
            action={<Shield className="h-4 w-4" style={{ color: 'var(--green)' }} aria-hidden="true" />}
          />
        </div>
        <hr className="divider" />
        <StackTable items={FRONTEND_STACK} />
      </Card>
    </div>

    {/* Architecture layers */}
    <Card>
      <CardHeader
        title="Architectural Layers"
        description="Request traversal path from HTTP boundary to data store."
      />
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {ARCH_LAYERS.map(({ icon, title, desc }) => (
          <div
            key={title}
            className="rounded-md border p-4"
            style={{ borderColor: 'var(--border)', background: 'var(--bg-page)' }}
          >
            <div className="flex items-center gap-2 mb-2" style={{ color: 'var(--accent)' }}>
              {icon}
              <span className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>{title}</span>
            </div>
            <p className="text-xs" style={{ color: 'var(--text-secondary)', lineHeight: '1.6', margin: 0 }}>{desc}</p>
          </div>
        ))}
      </div>
    </Card>
  </div>
);
