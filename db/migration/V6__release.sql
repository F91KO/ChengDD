SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `cdd_template_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `template_version` varchar(32) NOT NULL COMMENT '模板版本',
  `template_type` varchar(32) NOT NULL COMMENT '模板类型',
  `runtime_package_url` varchar(512) DEFAULT NULL COMMENT '运行包地址',
  `source_package_url` varchar(512) DEFAULT NULL COMMENT '源码包地址',
  `change_summary` varchar(1024) DEFAULT NULL COMMENT '变更摘要',
  `status` varchar(32) NOT NULL COMMENT '模板状态',
  `released_at` datetime DEFAULT NULL COMMENT '模板发布时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_version_deleted` (`template_code`, `template_version`, `deleted`),
  KEY `idx_template_status` (`template_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板版本表';

CREATE TABLE IF NOT EXISTS `cdd_template_change_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `template_version_id` bigint NOT NULL COMMENT '模板版本ID',
  `change_type` varchar(32) NOT NULL COMMENT '变更类型',
  `change_summary` varchar(512) NOT NULL COMMENT '变更摘要',
  `change_detail` text COMMENT '变更详情',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_template_version_id` (`template_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板变更记录表';

CREATE TABLE IF NOT EXISTS `cdd_release_task` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_no` varchar(32) NOT NULL COMMENT '发布任务号',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `store_id` bigint NOT NULL COMMENT '店铺ID',
  `mini_program_id` bigint NOT NULL COMMENT '小程序ID',
  `template_version_id` bigint NOT NULL COMMENT '模板版本ID',
  `release_type` varchar(32) NOT NULL COMMENT '发布类型',
  `release_status` varchar(32) NOT NULL COMMENT '发布状态',
  `trigger_source` varchar(32) NOT NULL COMMENT '触发来源',
  `release_snapshot_json` json DEFAULT NULL COMMENT '发布快照',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '结束时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_no_deleted` (`task_no`, `deleted`),
  KEY `idx_merchant_status` (`merchant_id`, `release_status`),
  KEY `idx_store_created_at` (`store_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布任务主表';

CREATE TABLE IF NOT EXISTS `cdd_release_task_detail` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `step_code` varchar(64) NOT NULL COMMENT '步骤编码',
  `step_name` varchar(128) NOT NULL COMMENT '步骤名称',
  `step_status` varchar(32) NOT NULL COMMENT '步骤状态',
  `step_order` int NOT NULL DEFAULT '0' COMMENT '步骤顺序',
  `result_message` varchar(512) DEFAULT NULL COMMENT '执行结果',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '结束时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_step_code_deleted` (`task_id`, `step_code`, `deleted`),
  KEY `idx_task_step_order` (`task_id`, `step_order`),
  KEY `idx_task_step_status` (`task_id`, `step_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布任务步骤表';

CREATE TABLE IF NOT EXISTS `cdd_release_gray_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `gray_version` varchar(32) NOT NULL COMMENT '灰度版本',
  `gray_scope_json` json DEFAULT NULL COMMENT '灰度范围',
  `gray_status` varchar(32) NOT NULL COMMENT '灰度状态',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `ended_at` datetime DEFAULT NULL COMMENT '结束时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_gray_status` (`gray_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灰度发布记录表';

CREATE TABLE IF NOT EXISTS `cdd_release_rollback_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `rollback_target_version` varchar(32) NOT NULL COMMENT '回滚目标版本',
  `rollback_reason` varchar(512) DEFAULT NULL COMMENT '回滚原因',
  `rollback_status` varchar(32) NOT NULL COMMENT '回滚状态',
  `rolled_back_at` datetime DEFAULT NULL COMMENT '回滚时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_rollback_status` (`rollback_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回滚记录表';

CREATE TABLE IF NOT EXISTS `cdd_release_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `log_level` varchar(16) NOT NULL COMMENT '日志级别',
  `log_stage` varchar(64) DEFAULT NULL COMMENT '日志阶段',
  `log_content` text COMMENT '日志内容',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_task_created_at` (`task_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布日志表';
