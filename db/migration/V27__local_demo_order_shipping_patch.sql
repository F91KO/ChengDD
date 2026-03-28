SET NAMES utf8mb4;

UPDATE `cdd_order_info`
SET
  `order_status` = 'shipped',
  `pay_status` = 'paid',
  `delivery_status` = 'shipped',
  `logistics_company_code` = 'SF',
  `logistics_company_name` = '顺丰速运',
  `tracking_no` = 'SF202603220001',
  `paid_at` = '2026-03-22 09:18:00',
  `shipped_at` = '2026-03-22 10:35:00',
  `finished_at` = NULL,
  `updated_by` = 1001,
  `updated_at` = '2026-03-22 10:35:00'
WHERE `order_no` = 'CDD202603220001'
  AND `deleted` = 0;

UPDATE `cdd_order_info`
SET
  `order_status` = 'finished',
  `pay_status` = 'paid',
  `delivery_status` = 'received',
  `logistics_company_code` = 'JD',
  `logistics_company_name` = '京东物流',
  `tracking_no` = 'JD202603210002',
  `paid_at` = '2026-03-21 18:30:00',
  `shipped_at` = '2026-03-21 19:05:00',
  `finished_at` = '2026-03-21 20:15:00',
  `updated_by` = 1001,
  `updated_at` = '2026-03-21 20:15:00'
WHERE `order_no` = 'CDD202603220002'
  AND `deleted` = 0;

UPDATE `cdd_order_status_log`
SET
  `to_status` = 'pending_pay',
  `operate_type` = 'system',
  `operator_id` = 1001,
  `operator_name` = '商家管理员',
  `remark` = '演示订单创建',
  `updated_by` = 1001,
  `updated_at` = '2026-03-22 09:12:00'
WHERE `id` = 9181201
  AND `deleted` = 0;

UPDATE `cdd_order_status_log`
SET
  `from_status` = 'pending_pay',
  `to_status` = 'paid',
  `operate_type` = 'system',
  `operator_id` = 1001,
  `operator_name` = '商家管理员',
  `remark` = '演示订单支付完成',
  `updated_by` = 1001,
  `updated_at` = '2026-03-22 09:18:00'
WHERE `id` = 9181202
  AND `deleted` = 0;

INSERT INTO `cdd_order_status_log` (
  `id`, `order_id`, `order_no`, `from_status`, `to_status`, `operate_type`,
  `operator_id`, `operator_name`, `remark`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted`, `version`
)
SELECT
  9181204, 9181001, 'CDD202603220001', 'paid', 'shipped', 'ship',
  1001, '商家管理员', '演示订单已发货，物流单号 SF202603220001', 1001, 1001, '2026-03-22 10:35:00', '2026-03-22 10:35:00', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_order_status_log` WHERE `id` = 9181204 AND `deleted` = 0
);

UPDATE `cdd_order_status_log`
SET
  `from_status` = NULL,
  `to_status` = 'pending_pay',
  `operate_type` = 'system',
  `operator_id` = 1001,
  `operator_name` = '商家管理员',
  `remark` = '演示订单创建',
  `updated_by` = 1001,
  `updated_at` = '2026-03-21 18:10:00'
WHERE `id` = 9181205
  AND `deleted` = 0;

INSERT INTO `cdd_order_status_log` (
  `id`, `order_id`, `order_no`, `from_status`, `to_status`, `operate_type`,
  `operator_id`, `operator_name`, `remark`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted`, `version`
)
SELECT
  9181205, 9181002, 'CDD202603220002', NULL, 'pending_pay', 'system',
  1001, '商家管理员', '演示订单创建', 1001, 1001, '2026-03-21 18:10:00', '2026-03-21 18:10:00', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_order_status_log` WHERE `id` = 9181205 AND `deleted` = 0
);

INSERT INTO `cdd_order_status_log` (
  `id`, `order_id`, `order_no`, `from_status`, `to_status`, `operate_type`,
  `operator_id`, `operator_name`, `remark`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted`, `version`
)
SELECT
  9181206, 9181002, 'CDD202603220002', 'pending_pay', 'paid', 'system',
  1001, '商家管理员', '演示订单支付完成', 1001, 1001, '2026-03-21 18:30:00', '2026-03-21 18:30:00', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_order_status_log` WHERE `id` = 9181206 AND `deleted` = 0
);

UPDATE `cdd_order_status_log`
SET
  `from_status` = 'shipped',
  `to_status` = 'finished',
  `operate_type` = 'delivery_transition',
  `operator_id` = 1001,
  `operator_name` = '系统',
  `remark` = '演示订单已签收完成',
  `updated_by` = 1001,
  `updated_at` = '2026-03-21 20:15:00'
WHERE `id` = 9181203
  AND `deleted` = 0;

INSERT INTO `cdd_order_status_log` (
  `id`, `order_id`, `order_no`, `from_status`, `to_status`, `operate_type`,
  `operator_id`, `operator_name`, `remark`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted`, `version`
)
SELECT
  9181207, 9181002, 'CDD202603220002', 'paid', 'shipped', 'ship',
  1001, '商家管理员', '演示订单已发货，物流单号 JD202603210002', 1001, 1001, '2026-03-21 19:05:00', '2026-03-21 19:05:00', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_order_status_log` WHERE `id` = 9181207 AND `deleted` = 0
);
