import React from 'react';
import { Compass } from 'lucide-react';
import { EmptyState } from '../components/common/EmptyState';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="py-12 flex justify-center">
      <div className="max-w-md w-full">
        <EmptyState
          icon={<Compass className="h-8 w-8 text-slate-400" />}
          title="Page Not Found"
          description="The requested platform route does not exist. Check the URL or return to platform status."
          action={{
            label: "Return to Status",
            onClick: () => {
              window.location.href = "/";
            }
          }}
        />
      </div>
    </div>
  );
};
