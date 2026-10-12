import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import {
  authService,
  UserProfile,
  OrganizationInfo,
  OrganizationRole,
  LoginPayload,
  RegisterPayload,
} from '../services/authService';
import { getAuthToken, setAuthToken, ApiClientError } from '../services/apiClient';

interface AuthContextType {
  user: UserProfile | null;
  activeOrganization: OrganizationInfo | null;
  activeRole: OrganizationRole | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  switchOrganization: (orgId: string) => Promise<void>;
  logout: () => Promise<void>;
  clearError: () => void;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [activeOrganization, setActiveOrganization] = useState<OrganizationInfo | null>(null);
  const [activeRole, setActiveRole] = useState<OrganizationRole | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const clearError = useCallback(() => setError(null), []);

  const refreshUser = useCallback(async () => {
    const token = getAuthToken();
    if (!token) {
      setUser(null);
      setActiveOrganization(null);
      setActiveRole(null);
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      const res = await authService.getCurrentUser();
      setUser(res.user);
      setActiveOrganization(res.activeOrganization);
      setActiveRole(res.activeRole);
      setError(null);
    } catch (err) {
      if (err instanceof ApiClientError && err.status === 401) {
        setAuthToken(null);
        setUser(null);
        setActiveOrganization(null);
        setActiveRole(null);
      }
      // Non-critical on initial load if backend is uncontactable
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  const login = async (payload: LoginPayload) => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await authService.login(payload);
      setUser(res.user);
      setActiveOrganization(res.organization);
      setActiveRole(res.role);
    } catch (err) {
      const msg = err instanceof ApiClientError ? err.message : 'Login failed. Please check credentials.';
      setError(msg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (payload: RegisterPayload) => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await authService.register(payload);
      setUser(res.user);
      setActiveOrganization(res.organization);
      setActiveRole(res.role);
    } catch (err) {
      const msg = err instanceof ApiClientError ? err.message : 'Registration failed. Please check requirements.';
      setError(msg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const switchOrganization = async (orgId: string) => {
    setIsLoading(true);
    setError(null);
    try {
      await authService.switchOrganization(orgId);
      await refreshUser();
    } catch (err) {
      const msg = err instanceof ApiClientError ? err.message : 'Failed to switch organization.';
      setError(msg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    setIsLoading(true);
    try {
      await authService.logout();
    } finally {
      setUser(null);
      setActiveOrganization(null);
      setActiveRole(null);
      setError(null);
      setIsLoading(false);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        activeOrganization,
        activeRole,
        isAuthenticated: !!user && !!getAuthToken(),
        isLoading,
        error,
        login,
        register,
        switchOrganization,
        logout,
        clearError,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
