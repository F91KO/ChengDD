export interface OrderSummaryResponseRaw {
  order_no: string;
  merchant_id: number;
  store_id: number;
  user_id: number;
  order_status: string;
  pay_status: string;
  delivery_status: string;
  payable_amount: number | string;
  paid_amount: number | string;
  created_at: string;
}

