export interface MerchantDashboardSnapshotResponseRaw {
  id: number;
  merchant_id: number;
  store_id: number;
  snapshot_time: string;
  dashboard_payload: Record<string, unknown>;
}

export interface ReportOrderDailyResponseRaw {
  id: number;
  merchant_id: number;
  store_id: number;
  stat_date: string;
  order_count: number;
  paid_order_count: number;
  gross_amount: number | string;
  refund_amount: number | string;
}

export interface ReportHomeEventDailyResponseRaw {
  id: number;
  merchant_id: number;
  store_id: number;
  stat_date: string;
  mini_program_id: number | null;
  page_view_count: number;
  visitor_count: number;
  click_count: number;
}
