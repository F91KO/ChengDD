SET NAMES utf8mb4;

INSERT INTO `cdd_report_home_event_daily` (
  `id`, `merchant_id`, `store_id`, `stat_date`, `mini_program_id`,
  `page_view_count`, `visitor_count`, `click_count`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9701001, 1001, 1001, '2026-03-16', 1001,
  9620, 2840, 702,
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_report_home_event_daily`
  WHERE `store_id` = 1001 AND `stat_date` = '2026-03-16' AND `deleted` = 0
);

INSERT INTO `cdd_report_home_event_daily` (
  `id`, `merchant_id`, `store_id`, `stat_date`, `mini_program_id`,
  `page_view_count`, `visitor_count`, `click_count`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9701002, 1001, 1001, '2026-03-17', 1001,
  9180, 2710, 665,
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_report_home_event_daily`
  WHERE `store_id` = 1001 AND `stat_date` = '2026-03-17' AND `deleted` = 0
);

INSERT INTO `cdd_report_home_event_daily` (
  `id`, `merchant_id`, `store_id`, `stat_date`, `mini_program_id`,
  `page_view_count`, `visitor_count`, `click_count`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9701003, 1001, 1001, '2026-03-18', 1001,
  10150, 2988, 744,
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_report_home_event_daily`
  WHERE `store_id` = 1001 AND `stat_date` = '2026-03-18' AND `deleted` = 0
);

INSERT INTO `cdd_report_home_event_daily` (
  `id`, `merchant_id`, `store_id`, `stat_date`, `mini_program_id`,
  `page_view_count`, `visitor_count`, `click_count`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9701004, 1001, 1001, '2026-03-19', 1001,
  10980, 3250, 812,
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_report_home_event_daily`
  WHERE `store_id` = 1001 AND `stat_date` = '2026-03-19' AND `deleted` = 0
);

INSERT INTO `cdd_report_home_event_daily` (
  `id`, `merchant_id`, `store_id`, `stat_date`, `mini_program_id`,
  `page_view_count`, `visitor_count`, `click_count`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9701005, 1001, 1001, '2026-03-20', 1001,
  11320, 3386, 856,
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_report_home_event_daily`
  WHERE `store_id` = 1001 AND `stat_date` = '2026-03-20' AND `deleted` = 0
);

INSERT INTO `cdd_report_home_event_daily` (
  `id`, `merchant_id`, `store_id`, `stat_date`, `mini_program_id`,
  `page_view_count`, `visitor_count`, `click_count`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9701006, 1001, 1001, '2026-03-21', 1001,
  12080, 3610, 903,
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_report_home_event_daily`
  WHERE `store_id` = 1001 AND `stat_date` = '2026-03-21' AND `deleted` = 0
);

INSERT INTO `cdd_report_home_event_daily` (
  `id`, `merchant_id`, `store_id`, `stat_date`, `mini_program_id`,
  `page_view_count`, `visitor_count`, `click_count`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9701007, 1001, 1001, '2026-03-22', 1001,
  12800, 3860, 942,
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_report_home_event_daily`
  WHERE `store_id` = 1001 AND `stat_date` = '2026-03-22' AND `deleted` = 0
);

INSERT INTO `cdd_report_order_daily` (
  `id`, `merchant_id`, `store_id`, `stat_date`,
  `order_count`, `paid_order_count`, `gross_amount`, `refund_amount`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9702001, 1001, 1001, '2026-03-16',
  88, 79, 52000.00, 1200.00,
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_report_order_daily`
  WHERE `store_id` = 1001 AND `stat_date` = '2026-03-16' AND `deleted` = 0
);

INSERT INTO `cdd_report_order_daily` (
  `id`, `merchant_id`, `store_id`, `stat_date`,
  `order_count`, `paid_order_count`, `gross_amount`, `refund_amount`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9702002, 1001, 1001, '2026-03-17',
  82, 74, 48000.00, 900.00,
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_report_order_daily`
  WHERE `store_id` = 1001 AND `stat_date` = '2026-03-17' AND `deleted` = 0
);

INSERT INTO `cdd_report_order_daily` (
  `id`, `merchant_id`, `store_id`, `stat_date`,
  `order_count`, `paid_order_count`, `gross_amount`, `refund_amount`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9702003, 1001, 1001, '2026-03-18',
  96, 89, 65000.00, 1350.00,
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_report_order_daily`
  WHERE `store_id` = 1001 AND `stat_date` = '2026-03-18' AND `deleted` = 0
);

INSERT INTO `cdd_report_order_daily` (
  `id`, `merchant_id`, `store_id`, `stat_date`,
  `order_count`, `paid_order_count`, `gross_amount`, `refund_amount`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9702004, 1001, 1001, '2026-03-19',
  103, 96, 71000.00, 1600.00,
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_report_order_daily`
  WHERE `store_id` = 1001 AND `stat_date` = '2026-03-19' AND `deleted` = 0
);

INSERT INTO `cdd_report_order_daily` (
  `id`, `merchant_id`, `store_id`, `stat_date`,
  `order_count`, `paid_order_count`, `gross_amount`, `refund_amount`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9702005, 1001, 1001, '2026-03-20',
  98, 90, 67000.00, 1180.00,
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_report_order_daily`
  WHERE `store_id` = 1001 AND `stat_date` = '2026-03-20' AND `deleted` = 0
);

INSERT INTO `cdd_report_order_daily` (
  `id`, `merchant_id`, `store_id`, `stat_date`,
  `order_count`, `paid_order_count`, `gross_amount`, `refund_amount`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9702006, 1001, 1001, '2026-03-21',
  117, 108, 78600.00, 1420.00,
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_report_order_daily`
  WHERE `store_id` = 1001 AND `stat_date` = '2026-03-21' AND `deleted` = 0
);

INSERT INTO `cdd_report_order_daily` (
  `id`, `merchant_id`, `store_id`, `stat_date`,
  `order_count`, `paid_order_count`, `gross_amount`, `refund_amount`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9702007, 1001, 1001, '2026-03-22',
  128, 117, 82400.00, 1680.00,
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_report_order_daily`
  WHERE `store_id` = 1001 AND `stat_date` = '2026-03-22' AND `deleted` = 0
);

INSERT INTO `cdd_report_product_daily` (
  `id`, `merchant_id`, `store_id`, `stat_date`, `product_id`, `sku_id`,
  `view_count`, `sale_count`, `sale_amount`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9703001, 1001, 1001, '2026-03-22', 3100001, 3200001,
  1560, 84, 50316.00,
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_report_product_daily`
  WHERE `store_id` = 1001 AND `product_id` = 3100001 AND `stat_date` = '2026-03-22' AND `deleted` = 0
);

INSERT INTO `cdd_report_product_daily` (
  `id`, `merchant_id`, `store_id`, `stat_date`, `product_id`, `sku_id`,
  `view_count`, `sale_count`, `sale_amount`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9703002, 1001, 1001, '2026-03-22', 3100002, 3200002,
  920, 31, 12384.50,
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_report_product_daily`
  WHERE `store_id` = 1001 AND `product_id` = 3100002 AND `stat_date` = '2026-03-22' AND `deleted` = 0
);

INSERT INTO `cdd_metric_merchant_dashboard` (
  `id`, `merchant_id`, `store_id`, `snapshot_time`, `dashboard_payload_json`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9704001, 1001, 1001, '2026-03-22 10:30:00',
  JSON_OBJECT(
    'gmv', 82400.00,
    'order_count', 128,
    'paid_order_count', 117,
    'visitor_count', 3860,
    'click_count', 942,
    'active_product_count', 312,
    'pending_delivery_count', 42,
    'after_sale_processing_count', 5,
    'release_exception_count', 1,
    'todo_summary', JSON_ARRAY(
      JSON_OBJECT('title', '42 个待发货订单', 'detail', '高峰时段待仓配处理，建议优先查看今日履约队列。', 'tone', 'danger'),
      JSON_OBJECT('title', '5 个售后单处理中', 'detail', '退款与退货申请需要在 30 分钟内完成复核。', 'tone', 'default'),
      JSON_OBJECT('title', '春季专题馆已上线', 'detail', '营销会场已发布，可结合商品日报继续优化转化。', 'tone', 'info')
    )
  ),
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_metric_merchant_dashboard`
  WHERE `merchant_id` = 1001 AND `store_id` = 1001 AND `snapshot_time` = '2026-03-22 10:30:00' AND `deleted` = 0
);

INSERT INTO `cdd_metric_platform_dashboard` (
  `id`, `snapshot_time`, `dashboard_payload_json`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9705001, '2026-03-22 11:00:00',
  JSON_OBJECT(
    'merchant_count', 12,
    'daily_active_store_count', 8,
    'today_order_count', 368,
    'today_gmv', 246800.00
  ),
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_metric_platform_dashboard`
  WHERE `snapshot_time` = '2026-03-22 11:00:00' AND `deleted` = 0
);
