import React from 'react';
import { FlaskConical } from 'lucide-react';

/**
 * Persistent top-of-page banner that signals the application is running
 * in hackathon demo mode. Renders above the topbar, below the demo-mode
 * notice so it is immediately visible without being intrusive.
 */
export const DemoBanner: React.FC = () => (
  <div className="demo-banner" role="status" aria-label="Demo mode active">
    <FlaskConical className="h-3.5 w-3.5 flex-shrink-0" aria-hidden="true" />
    <span>
      <strong>Demo Mode</strong> — ByteXL Hackathon prototype. No live threat data is ingested.
      Backend connectivity verified via{' '}
      <code className="font-mono" style={{ fontSize: '0.6875rem' }}>/api/v1/health</code>.
    </span>
  </div>
);
