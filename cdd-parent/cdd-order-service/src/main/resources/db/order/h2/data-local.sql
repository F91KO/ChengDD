MERGE INTO cdd_order_info (
  id, order_no, merchant_id, store_id, user_id, checkout_snapshot_id,
  order_status, pay_status, delivery_status, buyer_remark,
  total_amount, discount_amount, payable_amount, paid_amount, delivery_fee_amount,
  receiver_name, receiver_mobile, receiver_address,
  logistics_company_code, logistics_company_name, tracking_no,
  paid_at, shipped_at, cancelled_at, finished_at,
  created_by, updated_by, deleted, version
)
KEY(id) VALUES
  (9181001, 'CDD202603220001', 1001, 1001, 1001, NULL,
   'shipped', 'paid', 'shipped', '工作日送达',
   59.90, 0.00, 59.90, 59.90, 0.00,
   '张三', '13800000000', '上海市浦东新区世纪大道 100 号',
   'SF', '顺丰速运', 'SF202603220001',
   TIMESTAMP '2026-03-22 09:18:00', TIMESTAMP '2026-03-22 10:35:00', NULL, NULL,
   1001, 1001, 0, 0),
  (9181002, 'CDD202603220002', 1001, 1001, 1001, NULL,
   'finished', 'paid', 'received', '已签收，口感不错',
   89.90, 10.00, 79.90, 79.90, 0.00,
   '张三', '13800000000', '上海市浦东新区世纪大道 100 号',
   'JD', '京东物流', 'JD202603210002',
   TIMESTAMP '2026-03-21 18:30:00', TIMESTAMP '2026-03-21 19:05:00', NULL, TIMESTAMP '2026-03-21 20:15:00',
   1001, 1001, 0, 0);

MERGE INTO cdd_order_item (
  id, order_id, merchant_id, store_id, product_id, sku_id,
  product_name, sku_name, sku_spec_json, sale_price, quantity, line_amount, refund_status,
  refunded_quantity, refunded_amount, created_by, updated_by, deleted, version
)
KEY(id) VALUES
  (9181101, 9181001, 1001, 1001, 3100001, 3200001,
   '赣南脐橙礼盒', '标准装', '{"规格":"12枚装"}', 59.90, 1, 59.90, 'none',
   0, 0.00, 1001, 1001, 0, 0),
  (9181102, 9181002, 1001, 1001, 3100002, 3200002,
   '冷萃茉莉花茶', '尝鲜装', '{"规格":"6瓶装"}', 34.95, 2, 69.90, 'partial_refunding',
   0, 0.00, 1001, 1001, 0, 0);

MERGE INTO cdd_order_status_log (
  id, order_id, order_no, from_status, to_status, operate_type,
  operator_id, operator_name, remark, created_by, updated_by, deleted, version
)
KEY(id) VALUES
  (9181201, 9181001, 'CDD202603220001', NULL, 'pending_pay', 'system',
   1001, '商家管理员', '演示订单创建', 1001, 1001, 0, 0),
  (9181202, 9181001, 'CDD202603220001', 'pending_pay', 'paid', 'system',
   1001, '商家管理员', '演示订单支付完成', 1001, 1001, 0, 0),
  (9181203, 9181002, 'CDD202603220002', 'shipped', 'finished', 'delivery_transition',
   1001, '系统', '演示订单已签收完成', 1001, 1001, 0, 0),
  (9181204, 9181001, 'CDD202603220001', 'paid', 'shipped', 'ship',
   1001, '商家管理员', '演示订单已发货，物流单号 SF202603220001', 1001, 1001, 0, 0),
  (9181205, 9181002, 'CDD202603220002', NULL, 'pending_pay', 'system',
   1001, '商家管理员', '演示订单创建', 1001, 1001, 0, 0),
  (9181206, 9181002, 'CDD202603220002', 'pending_pay', 'paid', 'system',
   1001, '商家管理员', '演示订单支付完成', 1001, 1001, 0, 0),
  (9181207, 9181002, 'CDD202603220002', 'paid', 'shipped', 'ship',
   1001, '商家管理员', '演示订单已发货，物流单号 JD202603210002', 1001, 1001, 0, 0);

MERGE INTO cdd_order_refund_record (
  id, refund_no, order_id, order_no, pay_record_id, after_sale_id, order_item_id, merchant_id, store_id,
  refund_reason, refund_status, refund_quantity, refund_amount, third_party_refund_no, applied_at,
  success_at, failure_reason, compensation_task_code, created_by, updated_by, deleted, version
)
KEY(id) VALUES
  (9181401, 'RF202603220001', 9181002, 'CDD202603220002', NULL, 9181304, 9181102, 1001, 1001,
   '售后退款处理中', 'processing', 1, 39.95, NULL, TIMESTAMP '2026-03-22 13:18:00',
   NULL, NULL, NULL, 1001, 1001, 0, 0);

MERGE INTO cdd_order_after_sale (
  id, after_sale_no, order_id, order_no, order_item_id, merchant_id, store_id, user_id,
  after_sale_type, after_sale_status, reason_code, reason_desc, proof_urls_json, refund_quantity, refund_amount,
  merchant_result, refund_record_id, refund_no, return_company, return_logistics_no, returned_at, approved_at,
  completed_at, closed_at, handled_by, handled_at, created_by, updated_by, deleted, version
)
KEY(id) VALUES
  (9181301, 'AS202603220001', 9181001, 'CDD202603220001', 9181101, 1001, 1001, 1001,
   'refund_only', 'pending_merchant', 'damaged', '商品破损', '[]', 1, 59.90,
   NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, NULL, NULL, 1001, 1001, 0, 0),
  (9181302, 'AS202603220002', 9181002, 'CDD202603220002', 9181102, 1001, 1001, 1001,
   'refund_only', 'rejected', 'not_match', '不满足售后规则', '[]', 1, 39.95,
   '不符合售后条件', NULL, NULL, NULL, NULL, NULL, NULL,
   NULL, NULL, 1001, TIMESTAMP '2026-03-22 11:05:00', 1001, 1001, 0, 0),
  (9181303, 'AS202603220003', 9181002, 'CDD202603220002', 9181102, 1001, 1001, 1001,
   'return_refund', 'waiting_return', 'quality_issue', '商品质量问题', '[]', 1, 39.95,
   '请寄回商品', NULL, NULL, NULL, NULL, NULL, TIMESTAMP '2026-03-22 12:10:00',
   NULL, NULL, 1001, TIMESTAMP '2026-03-22 12:10:00', 1001, 1001, 0, 0),
  (9181304, 'AS202603220004', 9181002, 'CDD202603220002', 9181102, 1001, 1001, 1001,
   'return_refund', 'refunding', 'quality_issue', '商品质量问题', '[]', 1, 39.95,
   '商家已收货，退款处理中', 9181401, 'RF202603220001', '顺丰速运', 'SFRET202603220001',
   TIMESTAMP '2026-03-22 13:12:00', TIMESTAMP '2026-03-22 12:45:00',
   NULL, NULL, 1001, TIMESTAMP '2026-03-22 12:45:00', 1001, 1001, 0, 0);
