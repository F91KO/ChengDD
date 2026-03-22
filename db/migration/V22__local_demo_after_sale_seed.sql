SET NAMES utf8mb4;

INSERT INTO `cdd_order_after_sale` (
  `id`, `after_sale_no`, `order_id`, `order_no`, `order_item_id`, `merchant_id`, `store_id`, `user_id`,
  `after_sale_type`, `after_sale_status`, `apply_reason`, `apply_desc`, `reason_code`, `reason_desc`,
  `proof_urls_json`, `refund_record_id`, `refund_no`, `refund_amount`, `refund_quantity`, `merchant_result`,
  `return_company`, `return_logistics_no`, `returned_at`, `approved_at`, `completed_at`, `closed_at`,
  `handled_by`, `handled_at`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted`, `version`
)
SELECT
  9181301, 'AS202603220001', 9181001, 'CDD202603220001', 9181101, 1001, 1001, 1001,
  'refund_only', 'pending_merchant', 'damaged', '商品破损', 'damaged', '商品破损',
  JSON_ARRAY(), NULL, NULL, 59.90, 1, NULL,
  NULL, NULL, NULL, NULL, NULL, NULL,
  NULL, NULL, 1001, 1001, '2026-03-22 10:10:00', '2026-03-22 10:10:00', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_order_after_sale` WHERE `id` = 9181301 AND `deleted` = 0
);

INSERT INTO `cdd_order_after_sale` (
  `id`, `after_sale_no`, `order_id`, `order_no`, `order_item_id`, `merchant_id`, `store_id`, `user_id`,
  `after_sale_type`, `after_sale_status`, `apply_reason`, `apply_desc`, `reason_code`, `reason_desc`,
  `proof_urls_json`, `refund_record_id`, `refund_no`, `refund_amount`, `refund_quantity`, `merchant_result`,
  `return_company`, `return_logistics_no`, `returned_at`, `approved_at`, `completed_at`, `closed_at`,
  `handled_by`, `handled_at`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted`, `version`
)
SELECT
  9181302, 'AS202603220002', 9181002, 'CDD202603220002', 9181102, 1001, 1001, 1001,
  'refund_only', 'rejected', 'not_match', '不满足售后规则', 'not_match', '不满足售后规则',
  JSON_ARRAY(), NULL, NULL, 39.95, 1, '不符合售后条件',
  NULL, NULL, NULL, NULL, NULL, '2026-03-22 11:05:00',
  1001, '2026-03-22 11:05:00', 1001, 1001, '2026-03-22 10:45:00', '2026-03-22 11:05:00', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_order_after_sale` WHERE `id` = 9181302 AND `deleted` = 0
);

INSERT INTO `cdd_order_after_sale` (
  `id`, `after_sale_no`, `order_id`, `order_no`, `order_item_id`, `merchant_id`, `store_id`, `user_id`,
  `after_sale_type`, `after_sale_status`, `apply_reason`, `apply_desc`, `reason_code`, `reason_desc`,
  `proof_urls_json`, `refund_record_id`, `refund_no`, `refund_amount`, `refund_quantity`, `merchant_result`,
  `return_company`, `return_logistics_no`, `returned_at`, `approved_at`, `completed_at`, `closed_at`,
  `handled_by`, `handled_at`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted`, `version`
)
SELECT
  9181303, 'AS202603220003', 9181002, 'CDD202603220002', 9181102, 1001, 1001, 1001,
  'return_refund', 'waiting_return', 'quality_issue', '商品质量问题', 'quality_issue', '商品质量问题',
  JSON_ARRAY(), NULL, NULL, 39.95, 1, '请寄回商品',
  NULL, NULL, NULL, '2026-03-22 12:10:00', NULL, NULL,
  1001, '2026-03-22 12:10:00', 1001, 1001, '2026-03-22 11:40:00', '2026-03-22 12:10:00', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_order_after_sale` WHERE `id` = 9181303 AND `deleted` = 0
);
