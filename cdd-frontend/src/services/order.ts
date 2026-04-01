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
      user_id: params.userId,
    },
  });
}

export async function shipOrder(payload: {
  orderNo: string;
  merchantId: number;
  storeId: number;
  userId: number;
  operatorId: number;
  logisticsCompanyCode: string;
  logisticsCompanyName: string;
  trackingNo: string;
  shipRemark?: string;
}) {
  return requestApi({
    method: 'POST',
    url: `/order/orders/${payload.orderNo}/ship`,
    data: {
      user_id: payload.userId,
      operator_id: payload.operatorId,
      logistics_company_code: payload.logisticsCompanyCode,
      logistics_company_name: payload.logisticsCompanyName,
      tracking_no: payload.trackingNo,
      ship_remark: payload.shipRemark ?? '',
    },
  });
}

export async function exportOrdersCsv(params: {
  merchantId: number;
  storeId: number;
  userId?: number | null;
  orderStatus?: string;
}): Promise<Blob> {
  const query = new URLSearchParams();
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
}): Promise<OrderAfterSaleLifecycleResponseRaw> {
  return requestApi<OrderAfterSaleLifecycleResponseRaw>({
    method: 'POST',
    url: `/order/after-sales/${payload.afterSaleNo}/review`,
    data: {
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
  operatorId: number;
  returnCompany: string;
  returnLogisticsNo: string;
}): Promise<OrderAfterSaleLifecycleResponseRaw> {
  return requestApi<OrderAfterSaleLifecycleResponseRaw>({
    method: 'POST',
    url: `/order/after-sales/${payload.afterSaleNo}/return`,
    data: {
      user_id: payload.userId,
      operator_id: payload.operatorId,
      return_company: payload.returnCompany,
      return_logistics_no: payload.returnLogisticsNo,
    },
  });
}
