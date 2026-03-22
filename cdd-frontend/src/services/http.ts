import axios, { AxiosHeaders } from 'axios';
import type { AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';
import { router } from '@/app/router';
import { ApiClientError, isApiResponseEnvelope, type ApiResponseEnvelope } from '@/types/api';
import type { TokenResponseRaw } from '@/types/auth';
import { clearAuthSession, readAccessToken, readRefreshToken, updateSessionTokens } from '@/services/session';

const SUCCESS_CODE = 0;
const UNAUTHORIZED_CODE = 40101;
const API_TIMEOUT = 12000;
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

type RetryableRequest = InternalAxiosRequestConfig & {
  _retry?: boolean;
};

const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
});

const refreshClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
});

let refreshPromise: Promise<string | null> | null = null;
let redirecting = false;

function buildClientError(
  fallbackMessage: string,
  options: {
    status?: number;
    code?: number;
    requestId?: string;
    payload?: unknown;
  } = {},
): ApiClientError {
  const { payload, status, code, requestId } = options;
  if (payload && isApiResponseEnvelope(payload)) {
    return new ApiClientError(payload.message || fallbackMessage, {
      status,
      code: payload.code,
      requestId: payload.request_id,
    });
  }

  return new ApiClientError(fallbackMessage, {
    status,
    code,
    requestId,
  });
}

async function redirectToLogin(): Promise<void> {
  clearAuthSession();
  if (redirecting) {
    return;
  }

  redirecting = true;
  try {
    if (router.currentRoute.value.path !== '/login') {
      await router.push('/login');
    }
  } finally {
    redirecting = false;
  }
}

async function refreshAccessToken(): Promise<string | null> {
  if (refreshPromise) {
    return refreshPromise;
  }

  refreshPromise = (async () => {
    const refreshToken = readRefreshToken();
    if (!refreshToken) {
      return null;
    }

    try {
      const response = await refreshClient.post<ApiResponseEnvelope<TokenResponseRaw>>(
        '/auth/token/refresh',
        {
          refresh_token: refreshToken,
        },
        {
          headers: {
            Authorization: `Bearer ${readAccessToken()}`,
            'X-Requested-With': 'XMLHttpRequest',
          },
        },
      );

      const payload = response.data;
      if (!isApiResponseEnvelope(payload) || payload.code !== SUCCESS_CODE || !payload.data?.access_token) {
        return null;
      }

      updateSessionTokens({
        accessToken: payload.data.access_token,
        refreshToken: payload.data.refresh_token,
        accessTokenExpiresAt: payload.data.access_token_expires_at,
        refreshTokenExpiresAt: payload.data.refresh_token_expires_at,
      });
      return payload.data.access_token;
    } catch {
      return null;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

http.interceptors.request.use((config) => {
  const headers = AxiosHeaders.from(config.headers);
  const token = readAccessToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  headers.set('X-Requested-With', 'XMLHttpRequest');
  config.headers = headers;
  return config;
});

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponseEnvelope<unknown>>) => {
    const request = error.config as RetryableRequest | undefined;
    const status = error.response?.status;
    const payload = error.response?.data;
    const payloadCode = payload && isApiResponseEnvelope(payload) ? payload.code : undefined;
    const canRetryUnauthorized = request && !request._retry && (status === 401 || payloadCode === UNAUTHORIZED_CODE);

    if (canRetryUnauthorized) {
      request._retry = true;
      const nextToken = await refreshAccessToken();
      if (nextToken) {
        const headers = AxiosHeaders.from(request.headers);
        headers.set('Authorization', `Bearer ${nextToken}`);
        request.headers = headers;
        return http.request(request);
      }
      await redirectToLogin();
      throw buildClientError('登录已过期，请重新登录。', { status, code: UNAUTHORIZED_CODE, payload });
    }

    if (!status) {
      throw new ApiClientError('网络连接失败，请检查服务是否已启动。');
    }

    throw buildClientError('请求失败，请稍后重试。', { status, payload });
  },
);

async function unwrapApiData<T>(payload: unknown, status?: number): Promise<T> {
  if (!isApiResponseEnvelope(payload)) {
    throw new ApiClientError('接口响应格式不正确，请联系后端排查。', { status });
  }

  if (payload.code === SUCCESS_CODE) {
    return payload.data as T;
  }

  if (payload.code === UNAUTHORIZED_CODE) {
    await redirectToLogin();
    throw new ApiClientError('登录已失效，请重新登录。', {
      code: payload.code,
      status,
      requestId: payload.request_id,
    });
  }

  throw new ApiClientError(payload.message || '接口调用失败。', {
    code: payload.code,
    status,
    requestId: payload.request_id,
  });
}

export async function requestApi<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request({
    ...config,
  });
  return unwrapApiData<T>(response.data, response.status);
}

export { http };
