import { requestApi } from '@/services/http';
import type { CurrentAuthContextRaw, TokenResponseRaw } from '@/types/auth';

export async function merchantLogin(payload: {
  accountName: string;
  password: string;
}): Promise<TokenResponseRaw> {
  return requestApi<TokenResponseRaw>({
    method: 'POST',
    url: '/auth/merchant/login',
    data: {
      account_name: payload.accountName,
      password: payload.password,
    },
  });
}

export async function fetchCurrentAuthContext(accessToken?: string): Promise<CurrentAuthContextRaw> {
  return requestApi<CurrentAuthContextRaw>({
    method: 'GET',
    url: '/auth/me',
    headers: accessToken
      ? {
          Authorization: `Bearer ${accessToken}`,
        }
      : undefined,
  });
}

export async function logoutByRefreshToken(refreshToken: string): Promise<void> {
  await requestApi<void>({
    method: 'POST',
    url: '/auth/logout',
    data: {
      refresh_token: refreshToken,
    },
  });
}
