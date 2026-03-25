import { requestApi } from '@/services/http';
import { readAccessToken } from '@/services/session';
import type { PageResponseRaw } from '@/types/page';
import type {
  OrderDetailResponseRaw,
  OrderStatusLogResponseRaw,
  OrderSummaryResponseRaw,
} from '@/types/order';
import type { OrderAfterSaleLifecycleResponseRaw } from '@/types/aftersales';

export async function fetchOrderList(params: {
  merchantId: number;
  storeId: number;
  userId?: number | null;
  orderStatus?: string;
  page?: number;
  pageSize?: number;
}): Promise<PageResponseRaw<OrderSummaryResponseRaw>> {
  return requestApi<PageResponseRaw<OrderSummaryResponseRaw>>({
    method: 'GET',
    url: '/order/orders',
    params: {
      merchant_id: params.merchantId,
      store_id: params.storeId,
      user_id: params.userId ?? undefined,
      order_status: params.orderStatus,
      page: params.page,
      page_size: params.pageSize,
    },
  });
}

export async function fetchOrderDetail(params: {
  orderNo: string;
  merchantId: number;
  storeId: number;
  userId: number;
}): Promise<OrderDetailResponseRaw> {
  return requestApi<OrderDetailResponseRaw>({
    method: 'GET',
    url: `/order/orders/${params.orderNo}`,
    params: {
      merchant_id: params.merchantId,
      store_id: params.storeId,
      user_id: params.userId,
    },
  });
}

export async function updateOrderDelivery(payload: {
  orderNo: string;
  merchantId: number;
  storeId: number;
  userId: number;
  deliveryStatus: string;
  remark?: string;
}) {
  return requestApi({
    method: 'POST',
    url: `/order/orders/${payload.orderNo}/delivery`,
    data: {
      merchant_id: payload.merchantId,
      store_id: payload.storeId,
      user_id: payload.userId,
      delivery_status: payload.deliveryStatus,
      remark: payload.remark ?? '',
    },
  });
}

export async function exportOrdersCsv(params: {
  merchantId: number;
  storeId: number;
  userId?: number | null;
  orderStatus?: string;
}): Promise<Blob> {
  const query = new URLSearchParams({
    merchant_id: String(params.merchantId),
    store_id: String(params.storeId),
  });
  if (params.userId != null) {
    query.set('user_id', String(params.userId));
  }
  if (params.orderStatus) {
    query.set('order_status', params.orderStatus);
  }
  const response = await fetch(`/api/order/orders/export?${query.toString()}`, {
    headers: {
      Authorization: `Bearer ${readAccessToken()}`,
      'X-Requested-With': 'XMLHttpRequest',
    },
  });
  if (!response.ok) {
    throw new Error(`订单导出失败（HTTP ${response.status}）`);
  }
  return response.blob();
}

export async function fetchOrderStatusLogs(params: {
  orderNo: string;
  merchantId: number;
  storeId: number;
  userId: number;
}): Promise<OrderStatusLogResponseRaw[]> {
  return requestApi<OrderStatusLogResponseRaw[]>({
    method: 'GET',
    url: `/order/orders/${params.orderNo}/status-logs`,
    params: {
      merchant_id: params.merchantId,
      store_id: params.storeId,
      user_id: params.userId,
    },
  });
}

export async function reviewAfterSale(payload: {
  afterSaleNo: string;
  merchantId: number;
  storeId: number;
  operatorId: number;
  reviewAction: string;
  merchantResult?: string;
}) {
  return requestApi({
    method: 'POST',
    url: `/order/after-sales/${payload.afterSaleNo}/review`,
    data: {
      merchant_id: payload.merchantId,
      store_id: payload.storeId,
      operator_id: payload.operatorId,
      review_action: payload.reviewAction,
      merchant_result: payload.merchantResult ?? '',
    },
  });
}

export async function submitAfterSaleReturn(payload: {
  afterSaleNo: string;
  merchantId: number;
  storeId: number;
  userId: number;
  returnCompany: string;
  returnLogisticsNo: string;
}): Promise<OrderAfterSaleLifecycleResponseRaw> {
  return requestApi<OrderAfterSaleLifecycleResponseRaw>({
    method: 'POST',
    url: `/order/after-sales/${payload.afterSaleNo}/return`,
    data: {
      merchant_id: payload.merchantId,
      store_id: payload.storeId,
      user_id: payload.userId,
      return_company: payload.returnCompany,
      return_logistics_no: payload.returnLogisticsNo,
    },
  });
}
