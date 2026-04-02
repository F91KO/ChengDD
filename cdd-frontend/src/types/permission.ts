import type { PageResponseRaw } from '@/types/page';

export interface MerchantSubAccountResponseRaw {
  account_id: string;
  account_name: string;
  display_name: string;
  mobile: string;
  remark?: string | null;
  status: 'enabled' | 'disabled';
  role_label?: string | null;
  permission_modules: string[];
  action_permissions: string[];
  data_scope_type: 'merchant' | 'store' | 'mini_program';
  data_scope_ids: string[];
}

export interface MerchantSubAccountResetLoginResponseRaw {
  account_id: string;
  temporary_password: string;
}

export interface MerchantSubAccountUpsertPayload {
  accountName: string;
  displayName: string;
  mobile: string;
  remark?: string;
  permissionModules: string[];
  actionPermissions: string[];
  dataScopeType: 'merchant' | 'store' | 'mini_program';
  dataScopeIds: string[];
}

export type MerchantSubAccountPageResponseRaw = PageResponseRaw<MerchantSubAccountResponseRaw>;
