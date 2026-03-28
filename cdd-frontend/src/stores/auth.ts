import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { fetchCurrentAuthContext, logoutByRefreshToken, merchantLogin } from '@/services/auth';
import { clearAuthSession, readAuthSession, writeAuthSession } from '@/services/session';
import type {
  AuthContext,
  AuthDisplayUser,
  AuthSession,
  CurrentAuthContextRaw,
  TokenResponseRaw,
} from '@/types/auth';

const DEFAULT_LOGIN_PASSWORD = import.meta.env.VITE_AUTH_DEFAULT_PASSWORD || 'merchant123456';

function parseNumericTail(raw: string | null | undefined): number | null {
  if (!raw) {
    return null;
  }

  const matched = raw.match(/(\d+)$/);
  if (!matched) {
    return null;
  }

  return Number(matched[1]);
}

function buildUser(context: AuthContext | null): AuthDisplayUser {
  if (!context) {
    return {
      merchantName: '当前商户',
      operatorName: '当前操作人',
      roleName: 'merchant_owner',
    };
  }

  return {
    merchantName: context.merchantId || '当前商户',
    operatorName: context.displayName || context.accountName,
    roleName: context.roleCodes[0] || 'merchant_owner',
  };
}

function normalizeContext(raw: CurrentAuthContextRaw): AuthContext {
  return {
    userId: raw.user_id,
    accountName: raw.account_name,
    displayName: raw.display_name,
    accountType: raw.account_type,
    merchantId: raw.merchant_id,
    storeId: raw.store_id,
    miniProgramId: raw.mini_program_id,
    roleCodes: raw.role_codes,
    tokenVersion: raw.token_version,
  };
}

function buildSessionFromRemote(tokenPayload: TokenResponseRaw, contextRaw: CurrentAuthContextRaw): AuthSession {
  const context = normalizeContext(contextRaw);
  return {
    accessToken: tokenPayload.access_token,
    refreshToken: tokenPayload.refresh_token,
    accessTokenExpiresAt: tokenPayload.access_token_expires_at,
    refreshTokenExpiresAt: tokenPayload.refresh_token_expires_at,
    authMode: 'remote',
    context,
    user: buildUser(context),
  };
}

export const useAuthStore = defineStore('auth', () => {
  const session = ref<AuthSession | null>(null);
  const initialized = ref(false);
  const authenticating = ref(false);
  const authNotice = ref('请先登录。');

  const isAuthenticated = computed(() => Boolean(session.value?.accessToken));
  const token = computed(() => session.value?.accessToken ?? '');
  const refreshTokenValue = computed(() => session.value?.refreshToken ?? '');
  const user = computed<AuthDisplayUser>(() => session.value?.user ?? buildUser(null));
  const authMode = computed<'remote' | 'anonymous'>(() => (session.value?.authMode === 'remote' ? 'remote' : 'anonymous'));
  const context = computed<AuthContext | null>(() => session.value?.context ?? null);

  const businessScope = computed(() => {
    const contextValue = context.value;
    const merchantId = parseNumericTail(contextValue?.merchantId);
    const storeId = parseNumericTail(contextValue?.storeId);
    const userId = parseNumericTail(contextValue?.userId);

    return {
      merchantId,
      storeId,
      userId,
      derivedFromContext: merchantId !== null && storeId !== null,
    };
  });

  const merchantIdForQuery = computed<number | null>(() => businessScope.value.merchantId);
  const storeIdForQuery = computed<number | null>(() => businessScope.value.storeId);
  const userIdForQuery = computed<number | null>(() => businessScope.value.userId);

  function setSession(nextSession: AuthSession | null, notice?: string) {
    session.value = nextSession;

    if (nextSession) {
      writeAuthSession(nextSession);
    } else {
      clearAuthSession();
    }

    if (notice) {
      authNotice.value = notice;
    }
  }

  function hydrate() {
    const next = readAuthSession() as (AuthSession & { authMode?: string }) | null;
    if (next?.authMode && next.authMode !== 'remote') {
      clearAuthSession();
      session.value = null;
      authNotice.value = '检测到旧登录会话格式，已清理，请重新登录。';
      initialized.value = true;
      return;
    }

    session.value = next as AuthSession | null;
    if (session.value?.authMode === 'remote') {
      authNotice.value = '已登录，可继续访问业务页面。';
    } else {
      authNotice.value = '请先登录。';
    }
    initialized.value = true;
  }

  async function login(payload: { account: string; password?: string }) {
    const accountName = payload.account?.trim() || 'merchant_admin';
    const password = payload.password?.trim() || DEFAULT_LOGIN_PASSWORD;
    authenticating.value = true;

    try {
      const tokenPayload = await merchantLogin({
        accountName,
        password,
      });
      const current = await fetchCurrentAuthContext(tokenPayload.access_token);
      setSession(buildSessionFromRemote(tokenPayload, current), '已登录，可继续访问业务页面。');
      return {
        mode: 'remote' as const,
        message: '登录成功。',
      };
    } finally {
      authenticating.value = false;
    }
  }

  async function refreshCurrentContext() {
    const current = await fetchCurrentAuthContext();
    const currentSession = session.value;
    if (!currentSession) {
      return current;
    }

    setSession(
      {
        ...currentSession,
        authMode: 'remote',
        context: normalizeContext(current),
        user: buildUser(normalizeContext(current)),
      },
      '已登录，可继续访问业务页面。',
    );
    return current;
  }

  async function ensureCurrentContext() {
    if (!session.value) {
      return null;
    }

    if (session.value.authMode !== 'remote' || session.value.context) {
      return session.value.context;
    }

    try {
      const current = await refreshCurrentContext();
      return normalizeContext(current);
    } catch (error) {
      authNotice.value = `获取当前身份失败：${error instanceof Error ? error.message : '未知错误'}`;
      return session.value.context;
    }
  }

  async function logout() {
    const refreshToken = session.value?.refreshToken;
    const currentMode = session.value?.authMode;

    if (refreshToken && currentMode === 'remote') {
      try {
        await logoutByRefreshToken(refreshToken);
      } catch {
        // 服务不可达时仅做本地清理。
      }
    }

    clearSessionState('登录已退出。');
  }

  function clearSessionState(notice = '登录已失效，请重新登录。') {
    authenticating.value = false;
    setSession(null, notice);
  }

  return {
    session,
    initialized,
    authenticating,
    isAuthenticated,
    token,
    refreshTokenValue,
    user,
    context,
    authMode,
    authNotice,
    businessScope,
    merchantIdForQuery,
    storeIdForQuery,
    userIdForQuery,
    hydrate,
    login,
    refreshCurrentContext,
    ensureCurrentContext,
    logout,
    clearSession: clearSessionState,
  };
});
