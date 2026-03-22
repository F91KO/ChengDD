import { requestApi } from '@/services/http';
import type { ProductSummaryResponseRaw } from '@/types/product';

export async function fetchProductList(params: {
  merchantId: number;
  storeId: number;
  status?: string;
}): Promise<ProductSummaryResponseRaw[]> {
  return requestApi<ProductSummaryResponseRaw[]>({
    method: 'GET',
    url: '/product/spu',
    params: {
      merchant_id: params.merchantId,
      store_id: params.storeId,
      status: params.status,
    },
  });
}

