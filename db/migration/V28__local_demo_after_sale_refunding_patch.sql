SET NAMES utf8mb4;

UPDATE `cdd_order_status_log`
SET
  `deleted` = 1,
  `updated_by` = 1001,
  `updated_at` = '2026-03-28 09:30:00'
WHERE `order_no` = 'CDD202603220001'
  AND `operate_type` = 'delivery_transition'
  AND `operator_name` = '用户1001'
  AND `deleted` = 0;

UPDATE `cdd_order_item`
SET
  `refund_status` = 'refunding',
  `refunded_quantity` = 0,
  `refunded_amount` = 0.00,
  `updated_by` = 1001,
  `updated_at` = '2026-03-22 13:18:00'
WHERE `id` = 9181102
  AND `deleted` = 0;

UPDATE `cdd_order_refund_record`
SET
  `order_id` = 9181002,
  `order_no` = 'CDD202603220002',
  `pay_record_id` = NULL,
  `after_sale_id` = 9181304,
  `order_item_id` = 9181102,
  `merchant_id` = 1001,
  `store_id` = 1001,
  `refund_reason` = '售后退款处理中',
  `refund_status` = 'processing',
  `refund_quantity` = 1,
  `refund_amount` = 39.95,
  `third_party_refund_no` = NULL,
  `applied_at` = '2026-03-22 13:18:00',
  `success_at` = NULL,
  `failure_reason` = NULL,
  `compensation_task_code` = NULL,
  `created_by` = 1001,
  `updated_by` = 1001,
  `created_at` = '2026-03-22 13:18:00',
  `updated_at` = '2026-03-22 13:18:00',
  `deleted` = 0,
  `version` = 0
WHERE `refund_no` = 'RF202603220001';

INSERT INTO `cdd_order_refund_record` (
  `id`, `refund_no`, `order_id`, `order_no`, `pay_record_id`, `after_sale_id`, `order_item_id`, `merchant_id`, `store_id`,
  `refund_reason`, `refund_status`, `refund_quantity`, `refund_amount`, `third_party_refund_no`, `applied_at`,
  `success_at`, `failure_reason`, `compensation_task_code`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted`, `version`
)
SELECT
  9181401, 'RF202603220001', 9181002, 'CDD202603220002', NULL, 9181304, 9181102, 1001, 1001,
  '售后退款处理中', 'processing', 1, 39.95, NULL, '2026-03-22 13:18:00',
  NULL, NULL, NULL, 1001, 1001, '2026-03-22 13:18:00', '2026-03-22 13:18:00', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_order_refund_record` WHERE `refund_no` = 'RF202603220001' AND `deleted` = 0
);

UPDATE `cdd_order_after_sale`
SET
  `order_id` = 9181002,
  `order_no` = 'CDD202603220002',
  `order_item_id` = 9181102,
  `merchant_id` = 1001,
  `store_id` = 1001,
  `user_id` = 1001,
  `after_sale_type` = 'return_refund',
  `after_sale_status` = 'refunding',
  `reason_code` = 'quality_issue',
  `reason_desc` = '商品质量问题',
  `proof_urls_json` = JSON_ARRAY(),
  `refund_quantity` = 1,
  `refund_amount` = 39.95,
  `merchant_result` = '商家已收货，退款处理中',
  `refund_record_id` = 9181401,
  `refund_no` = 'RF202603220001',
  `return_company` = '顺丰速运',
  `return_logistics_no` = 'SFRET202603220001',
  `returned_at` = '2026-03-22 13:12:00',
  `approved_at` = '2026-03-22 12:45:00',
  `completed_at` = NULL,
  `closed_at` = NULL,
  `handled_by` = 1001,
  `handled_at` = '2026-03-22 12:45:00',
  `created_by` = 1001,
  `updated_by` = 1001,
  `created_at` = '2026-03-22 12:20:00',
  `updated_at` = '2026-03-22 13:18:00',
  `deleted` = 0,
  `version` = 0
WHERE `after_sale_no` = 'AS202603220004';

INSERT INTO `cdd_order_after_sale` (
  `id`, `after_sale_no`, `order_id`, `order_no`, `order_item_id`, `merchant_id`, `store_id`, `user_id`,
  `after_sale_type`, `after_sale_status`, `reason_code`, `reason_desc`, `proof_urls_json`, `refund_quantity`, `refund_amount`,
  `merchant_result`, `refund_record_id`, `refund_no`, `return_company`, `return_logistics_no`, `returned_at`, `approved_at`,
  `completed_at`, `closed_at`, `handled_by`, `handled_at`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted`, `version`
)
SELECT
  9181304, 'AS202603220004', 9181002, 'CDD202603220002', 9181102, 1001, 1001, 1001,
  'return_refund', 'refunding', 'quality_issue', '商品质量问题', JSON_ARRAY(), 1, 39.95,
  '商家已收货，退款处理中', 9181401, 'RF202603220001', '顺丰速运', 'SFRET202603220001', '2026-03-22 13:12:00', '2026-03-22 12:45:00',
  NULL, NULL, 1001, '2026-03-22 12:45:00', 1001, 1001, '2026-03-22 12:20:00', '2026-03-22 13:18:00', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_order_after_sale` WHERE `after_sale_no` = 'AS202603220004' AND `deleted` = 0
);
