import { requestApi } from '@/services/http';
import type { OrderAfterSaleSummaryResponseRaw } from '@/types/aftersales';

export async function fetchAfterSaleList(params: {
  merchantId: number;
  storeId: number;
  afterSaleStatus?: string;
}): Promise<OrderAfterSaleSummaryResponseRaw[]> {
  return requestApi<OrderAfterSaleSummaryResponseRaw[]>({
    method: 'GET',
    url: '/order/after-sales',
    params: {
      merchant_id: params.merchantId,
      store_id: params.storeId,
      after_sale_status: params.afterSaleStatus,
    },
  });
}
