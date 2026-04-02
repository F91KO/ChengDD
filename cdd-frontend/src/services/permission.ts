import { requestApi } from '@/services/http';
import type {
  MerchantSubAccountPageResponseRaw,
  MerchantSubAccountResetLoginResponseRaw,
  MerchantSubAccountResponseRaw,
  MerchantSubAccountUpsertPayload,
} from '@/types/permission';

function toRequestBody(payload: MerchantSubAccountUpsertPayload) {
  return {
    account_name: payload.accountName,
    display_name: payload.displayName,
    mobile: payload.mobile,
    remark: payload.remark ?? '',
    permission_modules: payload.permissionModules,
    action_permissions: payload.actionPermissions,
    data_scope_type: payload.dataScopeType,
    data_scope_ids: payload.dataScopeIds,
  };
}

export async function fetchSubAccounts(params?: {
  page?: number;
  pageSize?: number;
}): Promise<MerchantSubAccountPageResponseRaw> {
  return requestApi<MerchantSubAccountPageResponseRaw>({
    method: 'GET',
    url: '/merchant/accounts/sub-accounts',
    params: {
      page: params?.page,
      page_size: params?.pageSize,
    },
  });
}

export async function createSubAccount(
  payload: MerchantSubAccountUpsertPayload,
): Promise<MerchantSubAccountResponseRaw> {
  return requestApi<MerchantSubAccountResponseRaw>({
    method: 'POST',
    url: '/merchant/accounts/sub-accounts',
    data: toRequestBody(payload),
  });
}

export async function updateSubAccount(
  accountId: string,
  payload: MerchantSubAccountUpsertPayload,
): Promise<MerchantSubAccountResponseRaw> {
  return requestApi<MerchantSubAccountResponseRaw>({
    method: 'PUT',
    url: `/merchant/accounts/sub-accounts/${accountId}`,
    data: toRequestBody(payload),
  });
}

export async function disableSubAccount(accountId: string): Promise<MerchantSubAccountResponseRaw> {
  return requestApi<MerchantSubAccountResponseRaw>({
    method: 'POST',
    url: `/merchant/accounts/sub-accounts/${accountId}/disable`,
  });
}

export async function resetSubAccountLogin(accountId: string): Promise<MerchantSubAccountResetLoginResponseRaw> {
  return requestApi<MerchantSubAccountResetLoginResponseRaw>({
    method: 'POST',
    url: `/merchant/accounts/sub-accounts/${accountId}/reset-login`,
  });
}
