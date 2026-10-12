import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { LoadingState } from '../common/LoadingState';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-slate-50">
        <LoadingState message="Verifying session authenticity..." />
      </div>
    );
  }

  if (!isAuthenticated) {
    // Redirect to login while preserving target path
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
};
