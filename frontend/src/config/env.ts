export interface EnvConfig {
  apiBaseUrl: string;
  appTitle: string;
  isProduction: boolean;
}

export const env: EnvConfig = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  appTitle: import.meta.env.VITE_APP_TITLE || 'Digital Risk Protection Platform',
  isProduction: import.meta.env.PROD,
};
