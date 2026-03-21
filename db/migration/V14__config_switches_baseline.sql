SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `cdd_config_kv_merchant_override` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `merchant_id` varchar(64) NOT NULL COMMENT '商家标识',
  `config_group` varchar(64) NOT NULL COMMENT '配置分组',
  `config_key` varchar(128) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '商家覆盖配置值',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_group_key_deleted` (`merchant_id`, `config_group`, `config_key`, `deleted`),
  KEY `idx_group_key` (`config_group`, `config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家配置覆盖表';

CREATE TABLE IF NOT EXISTS `cdd_config_feature_switch_merchant_override` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `switch_id` bigint NOT NULL COMMENT '功能开关ID',
  `merchant_id` varchar(64) NOT NULL COMMENT '商家标识',
  `switch_value` varchar(32) NOT NULL COMMENT '商家覆盖开关值',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_switch_merchant_deleted` (`switch_id`, `merchant_id`, `deleted`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家功能开关覆盖表';
