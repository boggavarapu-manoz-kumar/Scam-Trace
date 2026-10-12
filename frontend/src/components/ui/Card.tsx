import React from 'react';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  /** Apply standard internal padding. Defaults to true. */
  padding?: boolean;
}

export const Card: React.FC<CardProps> = ({ children, className = '', padding = true }) => (
  <div className={`card ${padding ? 'p-5' : ''} ${className}`}>
    {children}
  </div>
);

interface CardHeaderProps {
  title: string;
  description?: string;
  action?: React.ReactNode;
}

export const CardHeader: React.FC<CardHeaderProps> = ({ title, description, action }) => (
  <div className="flex items-start justify-between gap-4 mb-4">
    <div>
      <h3 className="text-sm font-semibold" style={{ color: 'var(--text-primary)', margin: 0 }}>
        {title}
      </h3>
      {description && (
        <p className="text-xs mt-0.5" style={{ color: 'var(--text-secondary)', margin: '0.25rem 0 0' }}>
          {description}
        </p>
      )}
    </div>
    {action && <div className="flex-shrink-0">{action}</div>}
  </div>
);
