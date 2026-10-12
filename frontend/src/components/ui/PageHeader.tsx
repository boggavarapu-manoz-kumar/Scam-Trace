import React from 'react';

interface PageHeaderProps {
  title: string;
  description?: string;
  actions?: React.ReactNode;
}

export const PageHeader: React.FC<PageHeaderProps> = ({ title, description, actions }) => (
  <div className="page-header">
    <div>
      <h1 className="page-title">{title}</h1>
      {description && <p className="page-description">{description}</p>}
    </div>
    {actions && (
      <div className="flex items-center gap-2 flex-shrink-0">
        {actions}
      </div>
    )}
  </div>
);
