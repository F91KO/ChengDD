import { AUTH_STORAGE_KEY, type AuthSession } from '@/types/auth';

function isBrowser(): boolean {
  return typeof window !== 'undefined';
}

export function readAuthSession(): AuthSession | null {
  if (!isBrowser()) {
    return null;
  }

  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as AuthSession;
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function writeAuthSession(session: AuthSession): void {
  if (!isBrowser()) {
    return;
  }

  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
}

export function clearAuthSession(): void {
  if (!isBrowser()) {
    return;
  }

  localStorage.removeItem(AUTH_STORAGE_KEY);
}

export function readAccessToken(): string {
  return readAuthSession()?.accessToken ?? '';
}

export function readRefreshToken(): string {
  return readAuthSession()?.refreshToken ?? '';
}

export function updateSessionTokens(tokens: {
  accessToken: string;
  refreshToken?: string;
  accessTokenExpiresAt?: string;
  refreshTokenExpiresAt?: string;
}): void {
  const current = readAuthSession();
  if (!current) {
    return;
  }

  writeAuthSession({
    ...current,
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken ?? current.refreshToken,
    accessTokenExpiresAt: tokens.accessTokenExpiresAt ?? current.accessTokenExpiresAt,
    refreshTokenExpiresAt: tokens.refreshTokenExpiresAt ?? current.refreshTokenExpiresAt,
  });
}

