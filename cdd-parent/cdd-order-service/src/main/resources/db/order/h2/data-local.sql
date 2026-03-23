MERGE INTO cdd_order_info (
  id, order_no, merchant_id, store_id, user_id, checkout_snapshot_id,
  order_status, pay_status, delivery_status, buyer_remark,
  total_amount, discount_amount, payable_amount, paid_amount, delivery_fee_amount,
  receiver_name, receiver_mobile, receiver_address, paid_at, cancelled_at, finished_at,
  created_by, updated_by, deleted, version
)
KEY(id) VALUES
  (9181001, 'CDD202603220001', 1001, 1001, 1001, NULL,
   'paid', 'paid', 'pending', '工作日送达',
   59.90, 0.00, 59.90, 59.90, 0.00,
   '张三', '13800000000', '上海市浦东新区世纪大道 100 号', TIMESTAMP '2026-03-22 09:18:00', NULL, NULL,
   1001, 1001, 0, 0),
  (9181002, 'CDD202603220002', 1001, 1001, 1001, NULL,
   'completed', 'paid', 'delivered', '已签收，口感不错',
   79.90, 10.00, 69.90, 69.90, 0.00,
   '张三', '13800000000', '上海市浦东新区世纪大道 100 号', TIMESTAMP '2026-03-21 18:30:00', NULL, TIMESTAMP '2026-03-21 20:15:00',
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
   '冷萃茉莉花茶', '尝鲜装', '{"规格":"6瓶装"}', 34.95, 2, 69.90, 'none',
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
  (9181203, 9181002, 'CDD202603220002', NULL, 'completed', 'system',
   1001, '商家管理员', '演示订单已完成', 1001, 1001, 0, 0);
