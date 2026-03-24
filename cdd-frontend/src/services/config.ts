import { requestApi } from '@/services/http';
import type {
  ConfigKvValueResponseRaw,
  ConfigPublishRecordResponseRaw,
  FeatureSwitchValueResponseRaw,
} from '@/types/config';

export async function fetchMerchantFeatureSwitches(merchantId: string): Promise<FeatureSwitchValueResponseRaw[]> {
  return requestApi<FeatureSwitchValueResponseRaw[]>({
    method: 'GET',
    url: '/config/merchant/feature-switches',
    params: {
      merchant_id: merchantId,
    },
  });
}

export async function fetchEffectiveConfig(params: {
  merchantId?: string | null;
  configGroup: string;
  configKey: string;
}): Promise<ConfigKvValueResponseRaw> {
  return requestApi<ConfigKvValueResponseRaw>({
    method: 'GET',
    url: '/config/platform/kv/effective',
    params: {
      merchant_id: params.merchantId ?? undefined,
      config_group: params.configGroup,
      config_key: params.configKey,
    },
  });
}

export async function upsertPlatformConfig(payload: {
  configGroup: string;
  configKey: string;
  configValue: string;
  configDesc?: string;
}): Promise<ConfigKvValueResponseRaw> {
  return requestApi<ConfigKvValueResponseRaw>({
    method: 'POST',
    url: '/config/platform/kv',
    data: {
      config_group: payload.configGroup,
      config_key: payload.configKey,
      config_value: payload.configValue,
      config_desc: payload.configDesc ?? '',
    },
  });
}

export async function upsertMerchantConfigOverride(payload: {
  merchantId: string;
  configGroup: string;
  configKey: string;
  configValue: string;
}): Promise<ConfigKvValueResponseRaw> {
  return requestApi<ConfigKvValueResponseRaw>({
    method: 'POST',
    url: '/config/platform/kv/merchant-overrides',
    data: {
      merchant_id: payload.merchantId,
      config_group: payload.configGroup,
      config_key: payload.configKey,
      config_value: payload.configValue,
    },
  });
}

export async function listPlatformFeatureSwitches(): Promise<FeatureSwitchValueResponseRaw[]> {
  return requestApi<FeatureSwitchValueResponseRaw[]>({
    method: 'GET',
    url: '/config/platform/feature-switches',
  });
}

export async function getMerchantFeatureSwitch(switchCode: string, merchantId: string): Promise<FeatureSwitchValueResponseRaw> {
  return requestApi<FeatureSwitchValueResponseRaw>({
    method: 'GET',
    url: `/config/merchant/feature-switches/${switchCode}`,
    params: {
      merchant_id: merchantId,
    },
  });
}

export async function getPlatformFeatureSwitch(switchCode: string): Promise<FeatureSwitchValueResponseRaw> {
  return requestApi<FeatureSwitchValueResponseRaw>({
    method: 'GET',
    url: `/config/platform/feature-switches/${switchCode}`,
  });
}

export async function upsertFeatureSwitch(payload: {
  switchCode: string;
  switchName: string;
  switchScope: string;
  defaultValue: string;
  status: string;
}): Promise<FeatureSwitchValueResponseRaw> {
  return requestApi<FeatureSwitchValueResponseRaw>({
    method: 'POST',
    url: '/config/platform/feature-switches',
    data: {
      switch_code: payload.switchCode,
      switch_name: payload.switchName,
      switch_scope: payload.switchScope,
      default_value: payload.defaultValue,
      status: payload.status,
    },
  });
}

export async function changeFeatureSwitchStatus(payload: {
  switchCode: string;
  status: string;
}): Promise<FeatureSwitchValueResponseRaw> {
  return requestApi<FeatureSwitchValueResponseRaw>({
    method: 'POST',
    url: `/config/platform/feature-switches/${payload.switchCode}/change-status`,
    data: {
      status: payload.status,
    },
  });
}

export async function changeMerchantFeatureSwitch(payload: {
  merchantId: string;
  switchCode: string;
  switchValue: string;
}): Promise<FeatureSwitchValueResponseRaw> {
  return requestApi<FeatureSwitchValueResponseRaw>({
    method: 'POST',
    url: `/config/merchant/feature-switches/${payload.switchCode}/change`,
    data: {
      merchant_id: payload.merchantId,
      switch_value: payload.switchValue,
    },
  });
}

export async function fetchPublishRecords(params: {
  merchantId: string;
  storeId?: string;
}): Promise<ConfigPublishRecordResponseRaw[]> {
  return requestApi<ConfigPublishRecordResponseRaw[]>({
    method: 'GET',
    url: '/config/publish-records',
    params: {
      merchant_id: params.merchantId,
      store_id: params.storeId,
    },
  });
}

export async function fetchPublishRecord(taskNo: string): Promise<ConfigPublishRecordResponseRaw> {
  return requestApi<ConfigPublishRecordResponseRaw>({
    method: 'GET',
    url: `/config/publish-records/${taskNo}`,
  });
}

export async function createPublishRecord(payload: {
  merchantId: string;
  storeId: string;
  operatorName: string;
  publishNote: string;
}): Promise<ConfigPublishRecordResponseRaw> {
  return requestApi<ConfigPublishRecordResponseRaw>({
    method: 'POST',
    url: '/config/publish-records',
    data: {
      merchant_id: payload.merchantId,
      store_id: payload.storeId,
      operator_name: payload.operatorName,
      publish_note: payload.publishNote,
    },
  });
}

export async function rollbackPublishRecord(payload: {
  taskNo: string;
  operatorName: string;
  rollbackReason: string;
}): Promise<ConfigPublishRecordResponseRaw> {
  return requestApi<ConfigPublishRecordResponseRaw>({
    method: 'POST',
    url: `/config/publish-records/${payload.taskNo}/rollback`,
    data: {
      operator_name: payload.operatorName,
      rollback_reason: payload.rollbackReason,
    },
  });
}
