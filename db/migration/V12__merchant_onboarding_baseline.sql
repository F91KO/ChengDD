SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `cdd_merchant_onboarding_task` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_no` varchar(64) NOT NULL COMMENT '任务编号',
  `application_id` bigint NOT NULL COMMENT '入驻申请ID',
  `merchant_id` bigint DEFAULT NULL COMMENT '商家主体ID',
  `store_id` bigint DEFAULT NULL COMMENT '店铺ID',
  `mini_program_id` bigint DEFAULT NULL COMMENT '小程序接入配置ID',
  `task_status` varchar(32) NOT NULL COMMENT '任务状态',
  `step_code` varchar(64) NOT NULL COMMENT '当前步骤编码',
  `validation_result_json` json DEFAULT NULL COMMENT '接入参数校验结果',
  `error_message` varchar(512) DEFAULT NULL COMMENT '失败原因',
  `started_at` datetime NOT NULL COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '结束时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_no_deleted` (`task_no`, `deleted`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_task_status_started` (`task_status`, `started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家一键开通任务表';
