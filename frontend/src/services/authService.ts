import { apiClient, setAuthToken } from './apiClient';

export type OrganizationRole = 'OWNER' | 'ADMIN' | 'ANALYST' | 'VIEWER';

export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  active: boolean;
}

export interface OrganizationInfo {
  id: string;
  name: string;
  slug: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserProfile;
  organization: OrganizationInfo | null;
  role: OrganizationRole | null;
}

export interface CurrentUserResponse {
  user: UserProfile;
  activeOrganization: OrganizationInfo | null;
  activeRole: OrganizationRole | null;
}

export interface RegisterPayload {
  email: string;
  password: string;
  fullName: string;
  organizationName: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface SwitchOrgResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  activeOrganizationId: string;
  activeOrganizationName: string;
  activeRole: OrganizationRole;
}

export const authService = {
  async register(payload: RegisterPayload): Promise<AuthResponse> {
    const data = await apiClient<AuthResponse>('auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    setAuthToken(data.accessToken);
    return data;
  },

  async login(payload: LoginPayload): Promise<AuthResponse> {
    const data = await apiClient<AuthResponse>('auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    setAuthToken(data.accessToken);
    return data;
  },

  async getCurrentUser(): Promise<CurrentUserResponse> {
    return apiClient<CurrentUserResponse>('auth/me', {
      method: 'GET',
    });
  },

  async switchOrganization(orgId: string): Promise<SwitchOrgResponse> {
    const data = await apiClient<SwitchOrgResponse>(`organizations/${orgId}/switch`, {
      method: 'POST',
    });
    setAuthToken(data.accessToken);
    return data;
  },

  async logout(): Promise<void> {
    try {
      await apiClient<void>('auth/logout', { method: 'POST' });
    } catch {
      // Ignore network errors on logout to guarantee client cleanup
    } finally {
      setAuthToken(null);
    }
  },
};
