SET NAMES utf8mb4;

ALTER TABLE `cdd_release_task`
  ADD COLUMN `current_step_code` varchar(64) DEFAULT NULL COMMENT '当前执行步骤编码' AFTER `release_status`,
  ADD COLUMN `result_sync_status` varchar(32) NOT NULL DEFAULT 'pending' COMMENT '结果回写状态' AFTER `current_step_code`,
  ADD COLUMN `rollback_task_no` varchar(32) DEFAULT NULL COMMENT '回滚任务号' AFTER `result_sync_status`,
  ADD COLUMN `last_error_code` varchar(64) DEFAULT NULL COMMENT '最后错误码' AFTER `rollback_task_no`,
  ADD COLUMN `last_error_message` varchar(512) DEFAULT NULL COMMENT '最后错误信息' AFTER `last_error_code`;

ALTER TABLE `cdd_release_task`
  ADD KEY `idx_release_status_sync` (`release_status`, `result_sync_status`),
  ADD KEY `idx_mini_program_created_at` (`mini_program_id`, `created_at`);

ALTER TABLE `cdd_release_task_detail`
  ADD COLUMN `error_code` varchar(64) DEFAULT NULL COMMENT '步骤错误码' AFTER `result_message`,
  ADD COLUMN `retry_count` int NOT NULL DEFAULT '0' COMMENT '步骤重试次数' AFTER `error_code`;

CREATE TABLE IF NOT EXISTS `cdd_release_version_mapping` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `store_id` bigint NOT NULL COMMENT '店铺ID',
  `mini_program_id` bigint NOT NULL COMMENT '小程序ID',
  `template_version_id` bigint NOT NULL COMMENT '模板版本ID',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `template_version` varchar(32) NOT NULL COMMENT '模板版本号',
  `mapping_status` varchar(32) NOT NULL COMMENT '映射状态',
  `source_task_id` bigint NOT NULL COMMENT '来源发布任务ID',
  `activated_at` datetime NOT NULL COMMENT '生效时间',
  `deactivated_at` datetime DEFAULT NULL COMMENT '失效时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mini_program_template_deleted` (`mini_program_id`, `template_version_id`, `deleted`),
  KEY `idx_mini_program_mapping_status` (`mini_program_id`, `mapping_status`),
  KEY `idx_source_task_id` (`source_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序模板版本映射表';
