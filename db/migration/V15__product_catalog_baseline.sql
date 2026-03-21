SET NAMES utf8mb4;

ALTER TABLE `cdd_product_spu`
  ADD COLUMN `product_code` varchar(64) DEFAULT NULL COMMENT '商品编码' AFTER `product_name`;

ALTER TABLE `cdd_product_spu`
  ADD UNIQUE KEY `uk_store_product_code_deleted` (`store_id`, `product_code`, `deleted`),
  ADD KEY `idx_merchant_store_category_status` (`merchant_id`, `store_id`, `category_id`, `status`);

ALTER TABLE `cdd_product_stock`
  ADD COLUMN `updated_reason` varchar(128) DEFAULT NULL COMMENT '最近一次库存变更原因' AFTER `stock_status`;

CREATE TABLE IF NOT EXISTS `cdd_product_stock_change_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `store_id` bigint NOT NULL COMMENT '店铺ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `sku_id` bigint NOT NULL COMMENT 'SKU ID',
  `change_type` varchar(32) NOT NULL COMMENT '变更类型',
  `delta_stock` int NOT NULL COMMENT '库存变更量',
  `before_available_stock` int NOT NULL COMMENT '变更前可售库存',
  `after_available_stock` int NOT NULL COMMENT '变更后可售库存',
  `reason` varchar(128) DEFAULT NULL COMMENT '变更原因',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_sku_created_at` (`sku_id`, `created_at`),
  KEY `idx_product_created_at` (`product_id`, `created_at`),
  KEY `idx_merchant_store_created_at` (`merchant_id`, `store_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品库存变更日志表';

