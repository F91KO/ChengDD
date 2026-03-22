export interface OrderAfterSaleSummaryResponseRaw {
  after_sale_no: string;
  order_no: string;
  order_item_id: number | null;
  merchant_id: number;
  store_id: number;
  user_id: number;
  after_sale_type: string;
  after_sale_status: string;
  product_name: string | null;
  sku_name: string | null;
  refund_quantity: number | null;
  refund_amount: number | string | null;
  reason_code: string | null;
  reason_desc: string | null;
  merchant_result: string | null;
  refund_no: string | null;
  return_company: string | null;
  return_logistics_no: string | null;
  handled_by: number | null;
  handled_at: string | null;
  approved_at: string | null;
  returned_at: string | null;
  completed_at: string | null;
  updated_at: string;
}
