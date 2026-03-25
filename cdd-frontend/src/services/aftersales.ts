import { requestApi } from '@/services/http';
import type { PageResponseRaw } from '@/types/page';
import type {
  OrderAfterSaleDetailResponseRaw,
  OrderAfterSaleLogResponseRaw,
  OrderAfterSaleSummaryResponseRaw,
} from '@/types/aftersales';

export async function fetchAfterSaleList(params: {
  merchantId: number;
  storeId: number;
  afterSaleStatus?: string;
  page?: number;
  pageSize?: number;
}): Promise<PageResponseRaw<OrderAfterSaleSummaryResponseRaw>> {
  return requestApi<PageResponseRaw<OrderAfterSaleSummaryResponseRaw>>({
    method: 'GET',
    url: '/order/after-sales',
    params: {
      merchant_id: params.merchantId,
      store_id: params.storeId,
      after_sale_status: params.afterSaleStatus,
      page: params.page,
      page_size: params.pageSize,
    },
  });
}

export async function fetchAfterSaleDetail(params: {
  afterSaleNo: string;
  merchantId: number;
  storeId: number;
}): Promise<OrderAfterSaleDetailResponseRaw> {
  return requestApi<OrderAfterSaleDetailResponseRaw>({
    method: 'GET',
    url: `/order/after-sales/${params.afterSaleNo}`,
    params: {
      merchant_id: params.merchantId,
      store_id: params.storeId,
    },
  });
}

export async function fetchAfterSaleLogs(params: {
  afterSaleNo: string;
  merchantId: number;
  storeId: number;
}): Promise<OrderAfterSaleLogResponseRaw[]> {
  return requestApi<OrderAfterSaleLogResponseRaw[]>({
    method: 'GET',
    url: `/order/after-sales/${params.afterSaleNo}/logs`,
    params: {
      merchant_id: params.merchantId,
      store_id: params.storeId,
    },
  });
}
