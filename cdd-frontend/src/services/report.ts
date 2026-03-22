import { requestApi } from '@/services/http';
import type {
  MerchantDashboardSnapshotResponseRaw,
  ReportHomeEventDailyResponseRaw,
  ReportOrderDailyResponseRaw,
} from '@/types/report';

export async function fetchMerchantDashboardSnapshot(params: {
  merchantId: number;
  storeId: number;
}): Promise<MerchantDashboardSnapshotResponseRaw> {
  return requestApi<MerchantDashboardSnapshotResponseRaw>({
    method: 'GET',
    url: '/report/merchant-dashboard/latest',
    params: {
      merchant_id: params.merchantId,
      store_id: params.storeId,
    },
  });
}

export async function fetchOrderDailyList(params: {
  merchantId: number;
  storeId: number;
  startDate?: string;
  endDate?: string;
}): Promise<ReportOrderDailyResponseRaw[]> {
  return requestApi<ReportOrderDailyResponseRaw[]>({
    method: 'GET',
    url: '/report/orders/daily',
    params: {
      merchant_id: params.merchantId,
      store_id: params.storeId,
      start_date: params.startDate,
      end_date: params.endDate,
    },
  });
}

export async function fetchHomeEventDailyList(params: {
  merchantId: number;
  storeId: number;
  startDate?: string;
  endDate?: string;
}): Promise<ReportHomeEventDailyResponseRaw[]> {
  return requestApi<ReportHomeEventDailyResponseRaw[]>({
    method: 'GET',
    url: '/report/home-events/daily',
    params: {
      merchant_id: params.merchantId,
      store_id: params.storeId,
      start_date: params.startDate,
      end_date: params.endDate,
    },
  });
}
