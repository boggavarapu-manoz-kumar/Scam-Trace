import React from 'react';
import { AlertCircle, RefreshCw } from 'lucide-react';

interface ErrorStateProps {
  title?: string;
  message: string;
  onRetry?: () => void;
  className?: string;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'An error occurred',
  message,
  onRetry,
  className = '',
}) => {
  return (
    <div className={`rounded-lg border border-red-200 bg-red-50 p-6 text-red-900 ${className}`}>
      <div className="flex items-start gap-3">
        <AlertCircle className="h-5 w-5 text-red-600 shrink-0 mt-0.5" />
        <div className="flex-1">
          <h3 className="text-sm font-semibold">{title}</h3>
          <p className="mt-1 text-sm text-red-700">{message}</p>
          {onRetry && (
            <button
              type="button"
              onClick={onRetry}
              className="mt-3 inline-flex items-center gap-1.5 rounded-md bg-white px-3 py-1.5 text-xs font-semibold text-red-700 border border-red-300 hover:bg-red-50 transition-colors shadow-xs cursor-pointer"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              Retry Request
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
