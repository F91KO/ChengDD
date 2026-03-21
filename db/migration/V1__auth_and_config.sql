SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `cdd_auth_account` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `account_name` varchar(64) NOT NULL COMMENT '账号名',
  `display_name` varchar(64) NOT NULL COMMENT '显示名称',
  `mobile` varchar(32) DEFAULT NULL COMMENT '手机号',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `password_hash` varchar(256) DEFAULT NULL COMMENT '密码哈希',
  `account_type` varchar(32) NOT NULL COMMENT '账号类型',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_name_deleted` (`account_name`, `deleted`),
  KEY `idx_status` (`status`),
  KEY `idx_account_type` (`account_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认证账号表';

CREATE TABLE IF NOT EXISTS `cdd_auth_role` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `role_code` varchar(64) NOT NULL COMMENT '角色编码',
  `role_name` varchar(128) NOT NULL COMMENT '角色名称',
  `role_type` varchar(32) NOT NULL COMMENT '角色类型',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code_deleted` (`role_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色定义表';

CREATE TABLE IF NOT EXISTS `cdd_auth_permission` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `permission_code` varchar(64) NOT NULL COMMENT '权限编码',
  `permission_name` varchar(128) NOT NULL COMMENT '权限名称',
  `permission_group` varchar(64) DEFAULT NULL COMMENT '权限分组',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code_deleted` (`permission_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限点定义表';

CREATE TABLE IF NOT EXISTS `cdd_auth_role_permission` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission_deleted` (`role_id`, `permission_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关系表';

CREATE TABLE IF NOT EXISTS `cdd_audit_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人名称',
  `operator_type` varchar(32) DEFAULT NULL COMMENT '操作人类型',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型',
  `biz_id` varchar(64) DEFAULT NULL COMMENT '业务标识',
  `action` varchar(64) NOT NULL COMMENT '操作动作',
  `content` text COMMENT '审计内容',
  `ip_address` varchar(64) DEFAULT NULL COMMENT 'IP地址',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_biz_type_id` (`biz_type`, `biz_id`),
  KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

CREATE TABLE IF NOT EXISTS `cdd_config_feature_switch` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `switch_code` varchar(64) NOT NULL COMMENT '开关编码',
  `switch_name` varchar(128) NOT NULL COMMENT '开关名称',
  `switch_scope` varchar(32) NOT NULL COMMENT '作用范围',
  `default_value` varchar(32) NOT NULL COMMENT '默认值',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_switch_code_deleted` (`switch_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功能开关定义表';

CREATE TABLE IF NOT EXISTS `cdd_config_feature_switch_value` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `switch_id` bigint NOT NULL COMMENT '开关ID',
  `scope_type` varchar(32) NOT NULL COMMENT '范围类型',
  `scope_object_id` bigint NOT NULL COMMENT '对象ID',
  `switch_value` varchar(32) NOT NULL COMMENT '开关值',
  `effective_at` datetime DEFAULT NULL COMMENT '生效时间',
  `expired_at` datetime DEFAULT NULL COMMENT '失效时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_switch_scope_deleted` (`switch_id`, `scope_type`, `scope_object_id`, `deleted`),
  KEY `idx_scope_object` (`scope_type`, `scope_object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功能开关实例值表';

CREATE TABLE IF NOT EXISTS `cdd_config_preset_rule` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `rule_code` varchar(64) NOT NULL COMMENT '规则编码',
  `rule_name` varchar(128) NOT NULL COMMENT '规则名称',
  `rule_type` varchar(32) NOT NULL COMMENT '规则类型',
  `rule_payload_json` json DEFAULT NULL COMMENT '规则内容',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code_deleted` (`rule_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预置规则表';

CREATE TABLE IF NOT EXISTS `cdd_config_kv` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `config_group` varchar(64) NOT NULL COMMENT '配置分组',
  `config_key` varchar(128) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '配置值',
  `config_desc` varchar(512) DEFAULT NULL COMMENT '配置说明',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_key_deleted` (`config_group`, `config_key`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用配置表';

CREATE TABLE IF NOT EXISTS `cdd_config_id_segment` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `biz_code` varchar(64) NOT NULL COMMENT '业务编码',
  `current_max_id` bigint NOT NULL DEFAULT '0' COMMENT '当前最大ID',
  `step_size` int NOT NULL DEFAULT '1000' COMMENT '步长',
  `segment_desc` varchar(256) DEFAULT NULL COMMENT '号段说明',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_code_deleted` (`biz_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用侧发号号段表';
