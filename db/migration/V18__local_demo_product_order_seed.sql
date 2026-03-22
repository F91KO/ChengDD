SET NAMES utf8mb4;

INSERT INTO `cdd_product_category_template` (
  `id`, `template_name`, `industry_code`, `template_version`, `max_level`, `status`,
  `template_desc`, `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  2000001, '默认零售模板', 'retail', 'v1.0.0', 3, 'enabled',
  '一期默认分类模板', 1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_product_category_template` WHERE `id` = 2000001 AND `deleted` = 0
);

INSERT INTO `cdd_product_category` (
  `id`, `merchant_id`, `store_id`, `template_id`, `parent_id`, `category_name`, `category_level`,
  `sort_order`, `is_enabled`, `is_visible`, `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  2100001, 1001, 1001, 2000001, 0, '生鲜', 1,
  10, 1, 1, 1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_product_category` WHERE `id` = 2100001 AND `deleted` = 0
);

INSERT INTO `cdd_product_category` (
  `id`, `merchant_id`, `store_id`, `template_id`, `parent_id`, `category_name`, `category_level`,
  `sort_order`, `is_enabled`, `is_visible`, `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  2100002, 1001, 1001, 2000001, 2100001, '水果', 2,
  10, 1, 1, 1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_product_category` WHERE `id` = 2100002 AND `deleted` = 0
);

INSERT INTO `cdd_product_category` (
  `id`, `merchant_id`, `store_id`, `template_id`, `parent_id`, `category_name`, `category_level`,
  `sort_order`, `is_enabled`, `is_visible`, `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  2100003, 1001, 1001, 2000001, 0, '酒水饮料', 1,
  20, 1, 1, 1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_product_category` WHERE `id` = 2100003 AND `deleted` = 0
);

INSERT INTO `cdd_product_category` (
  `id`, `merchant_id`, `store_id`, `template_id`, `parent_id`, `category_name`, `category_level`,
  `sort_order`, `is_enabled`, `is_visible`, `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  2100004, 1001, 1001, 2000001, 2100003, '茶饮', 2,
  10, 1, 1, 1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_product_category` WHERE `id` = 2100004 AND `deleted` = 0
);

INSERT INTO `cdd_product_spu` (
  `id`, `merchant_id`, `store_id`, `category_id`, `product_name`, `product_code`, `product_sub_title`,
  `status`, `publish_check_status`, `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  3100001, 1001, 1001, 2100002, '赣南脐橙礼盒', 'CDD-ORANGE-SPU-001', '当季现发 12 枚装',
  'on_shelf', 'passed', 1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_product_spu` WHERE `id` = 3100001 AND `deleted` = 0
);

INSERT INTO `cdd_product_spu` (
  `id`, `merchant_id`, `store_id`, `category_id`, `product_name`, `product_code`, `product_sub_title`,
  `status`, `publish_check_status`, `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  3100002, 1001, 1001, 2100004, '冷萃茉莉花茶', 'CDD-TEA-SPU-001', '低糖配方 6 瓶装',
  'draft', 'passed', 1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_product_spu` WHERE `id` = 3100002 AND `deleted` = 0
);

INSERT INTO `cdd_product_spu` (
  `id`, `merchant_id`, `store_id`, `category_id`, `product_name`, `product_code`, `product_sub_title`,
  `status`, `publish_check_status`, `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  3100003, 1001, 1001, 2100004, '挂耳美式咖啡组合', 'CDD-COFFEE-SPU-001', '工作日醒神装 20 包',
  'off_shelf', 'passed', 1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_product_spu` WHERE `id` = 3100003 AND `deleted` = 0
);

INSERT INTO `cdd_product_sku` (
  `id`, `merchant_id`, `store_id`, `product_id`, `sku_code`, `sku_name`, `sale_price`, `status`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  3200001, 1001, 1001, 3100001, 'CDD-ORANGE-001', '标准装', 59.90, 'enabled',
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_product_sku` WHERE `id` = 3200001 AND `deleted` = 0
);

INSERT INTO `cdd_product_sku` (
  `id`, `merchant_id`, `store_id`, `product_id`, `sku_code`, `sku_name`, `sale_price`, `status`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  3200002, 1001, 1001, 3100002, 'CDD-TEA-001', '尝鲜装', 39.95, 'enabled',
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_product_sku` WHERE `id` = 3200002 AND `deleted` = 0
);

INSERT INTO `cdd_product_sku` (
  `id`, `merchant_id`, `store_id`, `product_id`, `sku_code`, `sku_name`, `sale_price`, `status`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  3200003, 1001, 1001, 3100003, 'CDD-COFFEE-001', '经典装', 49.90, 'enabled',
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_product_sku` WHERE `id` = 3200003 AND `deleted` = 0
);

INSERT INTO `cdd_product_stock` (
  `id`, `merchant_id`, `store_id`, `product_id`, `sku_id`, `available_stock`, `locked_stock`,
  `stock_status`, `updated_reason`, `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  3300001, 1001, 1001, 3100001, 3200001, 128, 0,
  'in_stock', '本地演示数据初始化', 1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_product_stock` WHERE `id` = 3300001 AND `deleted` = 0
);

INSERT INTO `cdd_product_stock` (
  `id`, `merchant_id`, `store_id`, `product_id`, `sku_id`, `available_stock`, `locked_stock`,
  `stock_status`, `updated_reason`, `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  3300002, 1001, 1001, 3100002, 3200002, 36, 0,
  'in_stock', '本地演示数据初始化', 1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_product_stock` WHERE `id` = 3300002 AND `deleted` = 0
);

INSERT INTO `cdd_product_stock` (
  `id`, `merchant_id`, `store_id`, `product_id`, `sku_id`, `available_stock`, `locked_stock`,
  `stock_status`, `updated_reason`, `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  3300003, 1001, 1001, 3100003, 3200003, 12, 0,
  'in_stock', '本地演示数据初始化', 1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_product_stock` WHERE `id` = 3300003 AND `deleted` = 0
);

INSERT INTO `cdd_order_info` (
  `id`, `order_no`, `merchant_id`, `store_id`, `user_id`, `checkout_snapshot_id`,
  `order_status`, `pay_status`, `delivery_status`, `buyer_remark`,
  `total_amount`, `discount_amount`, `payable_amount`, `paid_amount`, `delivery_fee_amount`,
  `receiver_name`, `receiver_mobile`, `receiver_address`, `paid_at`, `finished_at`,
  `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted`, `version`
)
SELECT
  9181001, 'CDD202603220001', 1001, 1001, 1001, NULL,
  'paid', 'paid', 'pending', '工作日送达',
  59.90, 0.00, 59.90, 59.90, 0.00,
  '张三', '13800000000', '上海市浦东新区世纪大道 100 号', '2026-03-22 09:18:00', NULL,
  1001, 1001, '2026-03-22 09:12:00', '2026-03-22 09:18:00', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_order_info` WHERE `order_no` = 'CDD202603220001' AND `deleted` = 0
);

INSERT INTO `cdd_order_info` (
  `id`, `order_no`, `merchant_id`, `store_id`, `user_id`, `checkout_snapshot_id`,
  `order_status`, `pay_status`, `delivery_status`, `buyer_remark`,
  `total_amount`, `discount_amount`, `payable_amount`, `paid_amount`, `delivery_fee_amount`,
  `receiver_name`, `receiver_mobile`, `receiver_address`, `paid_at`, `finished_at`,
  `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted`, `version`
)
SELECT
  9181002, 'CDD202603220002', 1001, 1001, 1001, NULL,
  'completed', 'paid', 'delivered', '已签收，口感不错',
  89.90, 10.00, 79.90, 79.90, 0.00,
  '张三', '13800000000', '上海市浦东新区世纪大道 100 号', '2026-03-21 18:30:00', '2026-03-21 20:15:00',
  1001, 1001, '2026-03-21 18:10:00', '2026-03-21 20:15:00', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_order_info` WHERE `order_no` = 'CDD202603220002' AND `deleted` = 0
);

INSERT INTO `cdd_order_item` (
  `id`, `order_id`, `merchant_id`, `store_id`, `product_id`, `sku_id`,
  `product_name`, `sku_name`, `sku_spec_json`, `sale_price`, `quantity`, `line_amount`,
  `refund_status`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted`, `version`
)
SELECT
  9181101, 9181001, 1001, 1001, 3100001, 3200001,
  '赣南脐橙礼盒', '标准装', JSON_OBJECT('规格', '12枚装'), 59.90, 1, 59.90,
  'none', 1001, 1001, '2026-03-22 09:12:00', '2026-03-22 09:18:00', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_order_item` WHERE `id` = 9181101 AND `deleted` = 0
);

INSERT INTO `cdd_order_item` (
  `id`, `order_id`, `merchant_id`, `store_id`, `product_id`, `sku_id`,
  `product_name`, `sku_name`, `sku_spec_json`, `sale_price`, `quantity`, `line_amount`,
  `refund_status`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted`, `version`
)
SELECT
  9181102, 9181002, 1001, 1001, 3100002, 3200002,
  '冷萃茉莉花茶', '尝鲜装', JSON_OBJECT('规格', '6瓶装'), 39.95, 2, 79.90,
  'none', 1001, 1001, '2026-03-21 18:10:00', '2026-03-21 20:15:00', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_order_item` WHERE `id` = 9181102 AND `deleted` = 0
);

INSERT INTO `cdd_order_status_log` (
  `id`, `order_id`, `order_no`, `from_status`, `to_status`, `operate_type`,
  `operator_id`, `operator_name`, `remark`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted`, `version`
)
SELECT
  9181201, 9181001, 'CDD202603220001', NULL, 'pending_pay', 'system',
  1001, '商家管理员', '演示订单创建', 1001, 1001, '2026-03-22 09:12:00', '2026-03-22 09:12:00', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_order_status_log` WHERE `id` = 9181201 AND `deleted` = 0
);

INSERT INTO `cdd_order_status_log` (
  `id`, `order_id`, `order_no`, `from_status`, `to_status`, `operate_type`,
  `operator_id`, `operator_name`, `remark`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted`, `version`
)
SELECT
  9181202, 9181001, 'CDD202603220001', 'pending_pay', 'paid', 'system',
  1001, '商家管理员', '演示订单支付完成', 1001, 1001, '2026-03-22 09:18:00', '2026-03-22 09:18:00', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_order_status_log` WHERE `id` = 9181202 AND `deleted` = 0
);

INSERT INTO `cdd_order_status_log` (
  `id`, `order_id`, `order_no`, `from_status`, `to_status`, `operate_type`,
  `operator_id`, `operator_name`, `remark`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted`, `version`
)
SELECT
  9181203, 9181002, 'CDD202603220002', NULL, 'completed', 'system',
  1001, '商家管理员', '演示订单已完成', 1001, 1001, '2026-03-21 20:15:00', '2026-03-21 20:15:00', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_order_status_log` WHERE `id` = 9181203 AND `deleted` = 0
);
