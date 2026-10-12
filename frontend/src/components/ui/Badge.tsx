import React from 'react';

export type BadgeVariant = 'green' | 'amber' | 'red' | 'blue' | 'slate';

interface BadgeProps {
  variant?: BadgeVariant;
  dot?: boolean;
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}

export const Badge: React.FC<BadgeProps> = ({
  variant = 'slate',
  dot = false,
  children,
  className = '',
  style,
}) => (
  <span className={`badge badge-${variant} ${className}`} style={style}>
    {dot && <span className="badge-dot" aria-hidden="true" />}
    {children}
  </span>
);
