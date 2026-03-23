export interface OrderSummaryResponseRaw {
  order_no: string;
  merchant_id: number;
  store_id: number;
  user_id: number;
  customer_identifier: string;
  channel: string;
  product_summary: string;
  order_status: string;
  pay_status: string;
  delivery_status: string;
  payable_amount: number | string;
  paid_amount: number | string;
  created_at: string;
}

export interface OrderStatusLogResponseRaw {
  from_status: string;
  to_status: string;
  operate_type: string;
  operator_id: number | null;
  operator_name: string | null;
  remark: string | null;
  created_at: string;
}

export interface OrderItemResponseRaw {
  id: number;
  order_id: number;
  product_id: number;
  sku_id: number;
  product_name: string;
  sku_name: string;
  sale_price: number | string;
  quantity: number;
  line_amount: number | string;
  refund_status: string | null;
  refunded_quantity: number | null;
  refunded_amount: number | string | null;
}

export interface OrderDetailResponseRaw {
  id: number;
  order_no: string;
  merchant_id: number;
  store_id: number;
  user_id: number;
  order_status: string;
  pay_status: string;
  delivery_status: string;
  buyer_remark: string | null;
  total_amount: number | string;
  discount_amount: number | string;
  payable_amount: number | string;
  paid_amount: number | string;
  delivery_fee_amount: number | string;
  receiver_name: string | null;
  receiver_mobile: string | null;
  receiver_address: string | null;
  created_at: string;
  paid_at: string | null;
  cancelled_at: string | null;
  finished_at: string | null;
  items: OrderItemResponseRaw[];
  status_logs: OrderStatusLogResponseRaw[];
}
