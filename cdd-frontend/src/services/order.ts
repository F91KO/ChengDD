import { requestApi } from '@/services/http';
import type { OrderSummaryResponseRaw } from '@/types/order';

export async function fetchOrderList(params: {
  merchantId: number;
  storeId: number;
  userId?: number | null;
  orderStatus?: string;
}): Promise<OrderSummaryResponseRaw[]> {
  return requestApi<OrderSummaryResponseRaw[]>({
    method: 'GET',
    url: '/order/orders',
    params: {
      merchant_id: params.merchantId,
      store_id: params.storeId,
      user_id: params.userId ?? undefined,
      order_status: params.orderStatus,
    },
  });
}

