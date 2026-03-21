SET NAMES utf8mb4;

ALTER TABLE `cdd_auth_account`
  ADD COLUMN `user_id` varchar(64) NOT NULL DEFAULT '' COMMENT '用户标识' AFTER `id`,
  ADD COLUMN `merchant_id` varchar(64) DEFAULT NULL COMMENT '商家标识' AFTER `account_type`,
  ADD COLUMN `store_id` varchar(64) DEFAULT NULL COMMENT '店铺标识' AFTER `merchant_id`,
  ADD COLUMN `mini_program_id` varchar(64) DEFAULT NULL COMMENT '小程序标识' AFTER `store_id`,
  ADD COLUMN `token_version` bigint NOT NULL DEFAULT '0' COMMENT '令牌版本号' AFTER `last_login_at`;

ALTER TABLE `cdd_auth_account`
  ADD UNIQUE KEY `uk_user_id_deleted` (`user_id`, `deleted`),
  ADD KEY `idx_account_type_status` (`account_type`, `status`);

CREATE TABLE IF NOT EXISTS `cdd_auth_account_role` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `account_id` bigint NOT NULL COMMENT '账号ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_role_deleted` (`account_id`, `role_id`, `deleted`),
  KEY `idx_account_id` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号角色关系表';

CREATE TABLE IF NOT EXISTS `cdd_auth_refresh_token_session` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `token_id` varchar(64) NOT NULL COMMENT '刷新令牌ID',
  `account_id` bigint NOT NULL COMMENT '账号ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户标识',
  `token_hash` varchar(128) NOT NULL COMMENT '刷新令牌哈希',
  `token_version` bigint NOT NULL DEFAULT '0' COMMENT '令牌版本号',
  `expires_at` datetime NOT NULL COMMENT '过期时间',
  `revoked_at` datetime DEFAULT NULL COMMENT '失效时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_id_deleted` (`token_id`, `deleted`),
  KEY `idx_user_id_deleted` (`user_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='刷新令牌会话表';
