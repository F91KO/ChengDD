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

export interface ReportProductDailyResponseRaw {
  id: number;
  merchant_id: number;
  store_id: number;
  stat_date: string;
  product_id: number;
  sku_id: number | null;
  view_count: number;
  sale_count: number;
  sale_amount: number | string;
}

export interface ReportHealthResponseRaw {
  merchant_id: number;
  store_id: number;
  ready: boolean;
  summary: string;
  items: ReportHealthItemResponseRaw[];
}

export interface ReportHealthItemResponseRaw {
  code: string;
  name: string;
  status: string;
  latest_data_time: string | null;
  message: string;
}
