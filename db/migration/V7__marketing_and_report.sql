SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `cdd_marketing_coupon` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `store_id` bigint NOT NULL COMMENT '店铺ID',
  `coupon_name` varchar(128) NOT NULL COMMENT '优惠券名称',
  `coupon_type` varchar(32) NOT NULL COMMENT '优惠券类型',
  `threshold_amount` decimal(10,2) DEFAULT NULL COMMENT '使用门槛',
  `discount_amount` decimal(10,2) DEFAULT NULL COMMENT '减免金额',
  `discount_rate` decimal(5,2) DEFAULT NULL COMMENT '折扣率',
  `issue_start_at` datetime DEFAULT NULL COMMENT '发放开始时间',
  `issue_end_at` datetime DEFAULT NULL COMMENT '发放结束时间',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_store_status` (`store_id`, `status`),
  KEY `idx_issue_end_at` (`issue_end_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券定义表';

CREATE TABLE IF NOT EXISTS `cdd_marketing_coupon_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `coupon_id` bigint NOT NULL COMMENT '优惠券ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `store_id` bigint NOT NULL COMMENT '店铺ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `record_status` varchar(32) NOT NULL COMMENT '记录状态',
  `received_at` datetime DEFAULT NULL COMMENT '领取时间',
  `used_at` datetime DEFAULT NULL COMMENT '使用时间',
  `order_id` bigint DEFAULT NULL COMMENT '订单ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_coupon_user` (`coupon_id`, `user_id`),
  KEY `idx_store_status` (`store_id`, `record_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户领券记录表';

CREATE TABLE IF NOT EXISTS `cdd_marketing_activity` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `store_id` bigint NOT NULL COMMENT '店铺ID',
  `activity_name` varchar(128) NOT NULL COMMENT '活动名称',
  `activity_type` varchar(32) NOT NULL COMMENT '活动类型',
  `activity_status` varchar(32) NOT NULL COMMENT '活动状态',
  `start_at` datetime DEFAULT NULL COMMENT '开始时间',
  `end_at` datetime DEFAULT NULL COMMENT '结束时间',
  `rule_payload_json` json DEFAULT NULL COMMENT '规则配置',
  `banner_url` varchar(512) DEFAULT NULL COMMENT '活动Banner',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_store_status` (`store_id`, `activity_status`),
  KEY `idx_store_time` (`store_id`, `start_at`, `end_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销活动表';

CREATE TABLE IF NOT EXISTS `cdd_marketing_activity_product` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `sku_id` bigint DEFAULT NULL COMMENT 'SKU ID',
  `activity_price` decimal(10,2) DEFAULT NULL COMMENT '活动价',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动商品绑定表';

CREATE TABLE IF NOT EXISTS `cdd_marketing_recommend_rule` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `store_id` bigint NOT NULL COMMENT '店铺ID',
  `rule_name` varchar(128) NOT NULL COMMENT '推荐规则名称',
  `scene_code` varchar(64) NOT NULL COMMENT '场景编码',
  `rule_payload_json` json DEFAULT NULL COMMENT '规则配置',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_store_scene` (`store_id`, `scene_code`),
  KEY `idx_store_status` (`store_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐规则表';

CREATE TABLE IF NOT EXISTS `cdd_marketing_topic_floor` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `store_id` bigint NOT NULL COMMENT '店铺ID',
  `topic_name` varchar(128) NOT NULL COMMENT '会场名称',
  `topic_code` varchar(64) NOT NULL COMMENT '会场编码',
  `banner_url` varchar(512) DEFAULT NULL COMMENT '会场头图',
  `floor_payload_json` json DEFAULT NULL COMMENT '楼层配置',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_topic_code_deleted` (`store_id`, `topic_code`, `deleted`),
  KEY `idx_store_status` (`store_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专题会场配置表';

CREATE TABLE IF NOT EXISTS `cdd_report_home_event_daily` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `store_id` bigint NOT NULL COMMENT '店铺ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `mini_program_id` bigint DEFAULT NULL COMMENT '小程序ID',
  `page_view_count` bigint NOT NULL DEFAULT '0' COMMENT '页面浏览量',
  `visitor_count` bigint NOT NULL DEFAULT '0' COMMENT '访客数',
  `click_count` bigint NOT NULL DEFAULT '0' COMMENT '点击量',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_stat_date_deleted` (`store_id`, `stat_date`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页事件日报表';

CREATE TABLE IF NOT EXISTS `cdd_report_order_daily` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `store_id` bigint NOT NULL COMMENT '店铺ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `order_count` bigint NOT NULL DEFAULT '0' COMMENT '订单数',
  `paid_order_count` bigint NOT NULL DEFAULT '0' COMMENT '支付订单数',
  `gross_amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '成交总额',
  `refund_amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '退款总额',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_order_stat_date_deleted` (`store_id`, `stat_date`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单日报表';

CREATE TABLE IF NOT EXISTS `cdd_report_product_daily` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `store_id` bigint NOT NULL COMMENT '店铺ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `sku_id` bigint DEFAULT NULL COMMENT 'SKU ID',
  `view_count` bigint NOT NULL DEFAULT '0' COMMENT '浏览量',
  `sale_count` bigint NOT NULL DEFAULT '0' COMMENT '销量',
  `sale_amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '销售额',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_product_stat_date_deleted` (`store_id`, `product_id`, `stat_date`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品日报表';

CREATE TABLE IF NOT EXISTS `cdd_metric_merchant_dashboard` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `store_id` bigint NOT NULL COMMENT '店铺ID',
  `snapshot_time` datetime NOT NULL COMMENT '快照时间',
  `dashboard_payload_json` json NOT NULL COMMENT '看板快照',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_store_snapshot_time` (`store_id`, `snapshot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家看板快照表';

CREATE TABLE IF NOT EXISTS `cdd_metric_platform_dashboard` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `snapshot_time` datetime NOT NULL COMMENT '快照时间',
  `dashboard_payload_json` json NOT NULL COMMENT '平台看板快照',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_snapshot_time` (`snapshot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台看板快照表';
