import { requestApi } from '@/services/http';
import type { ConfigKvValueResponseRaw, FeatureSwitchValueResponseRaw } from '@/types/config';

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
