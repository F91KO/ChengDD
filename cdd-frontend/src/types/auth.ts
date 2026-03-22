export const AUTH_STORAGE_KEY = 'cdd_admin_auth_session';

export interface TokenResponseRaw {
  token_type: string;
  account_type: string;
  access_token: string;
  refresh_token: string;
  access_token_expires_at: string;
  refresh_token_expires_at: string;
}

export interface CurrentAuthContextRaw {
  user_id: string;
  account_name: string;
  display_name: string;
  account_type: string;
  merchant_id: string | null;
  store_id: string | null;
  mini_program_id: string | null;
  role_codes: string[];
  token_version: number;
}

export interface AuthContext {
  userId: string;
  accountName: string;
  displayName: string;
  accountType: string;
  merchantId: string | null;
  storeId: string | null;
  miniProgramId: string | null;
  roleCodes: string[];
  tokenVersion: number;
}

export interface AuthDisplayUser {
  merchantName: string;
  operatorName: string;
  roleName: string;
}

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: string;
  refreshTokenExpiresAt: string;
  authMode: 'remote' | 'mock';
  context: AuthContext | null;
  user: AuthDisplayUser;
}

