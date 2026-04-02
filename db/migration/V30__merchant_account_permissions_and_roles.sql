SET NAMES utf8mb4;

ALTER TABLE `cdd_merchant_sub_account`
  ADD COLUMN `remark` varchar(255) DEFAULT NULL COMMENT '备注' AFTER `mobile`,
  ADD COLUMN `permission_modules_json` json DEFAULT NULL COMMENT '模块权限集合' AFTER `role_label`,
  ADD COLUMN `action_permissions_json` json DEFAULT NULL COMMENT '动作权限集合' AFTER `permission_modules_json`,
  ADD COLUMN `data_scope_type` varchar(32) DEFAULT NULL COMMENT '数据范围类型' AFTER `action_permissions_json`;

INSERT INTO `cdd_auth_role` (
  `id`, `role_code`, `role_name`, `role_type`, `status`, `created_by`, `updated_by`
)
SELECT 9200004, 'merchant_admin', '商家子账号', 'merchant', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role` WHERE `role_code` = 'merchant_admin' AND `deleted` = 0
);

INSERT INTO `cdd_auth_role_permission` (
  `id`, `role_id`, `permission_id`, `created_by`, `updated_by`
)
SELECT 9400012, 9200004, 9300005, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role_permission` WHERE `role_id` = 9200004 AND `permission_id` = 9300005 AND `deleted` = 0
);

INSERT INTO `cdd_auth_role_permission` (
  `id`, `role_id`, `permission_id`, `created_by`, `updated_by`
)
SELECT 9400013, 9200004, 9300006, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role_permission` WHERE `role_id` = 9200004 AND `permission_id` = 9300006 AND `deleted` = 0
);

INSERT INTO `cdd_auth_role_permission` (
  `id`, `role_id`, `permission_id`, `created_by`, `updated_by`
)
SELECT 9400014, 9200004, 9300007, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role_permission` WHERE `role_id` = 9200004 AND `permission_id` = 9300007 AND `deleted` = 0
);

INSERT INTO `cdd_auth_role_permission` (
  `id`, `role_id`, `permission_id`, `created_by`, `updated_by`
)
SELECT 9400015, 9200004, 9300008, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role_permission` WHERE `role_id` = 9200004 AND `permission_id` = 9300008 AND `deleted` = 0
);
