SET NAMES utf8mb4;

UPDATE `cdd_report_home_event_daily`
SET
  `mini_program_id` = 1001,
  `page_view_count` = 12800,
  `visitor_count` = 3860,
  `click_count` = 942,
  `updated_by` = 1001,
  `deleted` = 0
WHERE `merchant_id` = 1001
  AND `store_id` = 1001
  AND `stat_date` = '2026-03-22';

UPDATE `cdd_report_order_daily`
SET
  `order_count` = 128,
  `paid_order_count` = 117,
  `gross_amount` = 82400.00,
  `refund_amount` = 1680.00,
  `updated_by` = 1001,
  `deleted` = 0
WHERE `merchant_id` = 1001
  AND `store_id` = 1001
  AND `stat_date` = '2026-03-22';

UPDATE `cdd_report_product_daily`
SET
  `view_count` = 1560,
  `sale_count` = 84,
  `sale_amount` = 50316.00,
  `updated_by` = 1001,
  `deleted` = 0
WHERE `merchant_id` = 1001
  AND `store_id` = 1001
  AND `product_id` = 3100001
  AND `stat_date` = '2026-03-22';

INSERT INTO `cdd_metric_merchant_dashboard` (
  `id`, `merchant_id`, `store_id`, `snapshot_time`, `dashboard_payload_json`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9704002, 1001, 1001, '2026-03-22 12:00:00',
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
  WHERE `merchant_id` = 1001
    AND `store_id` = 1001
    AND `snapshot_time` = '2026-03-22 12:00:00'
    AND `deleted` = 0
);

INSERT INTO `cdd_metric_platform_dashboard` (
  `id`, `snapshot_time`, `dashboard_payload_json`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  9705002, '2026-03-22 12:00:00',
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
  WHERE `snapshot_time` = '2026-03-22 12:00:00'
    AND `deleted` = 0
);
