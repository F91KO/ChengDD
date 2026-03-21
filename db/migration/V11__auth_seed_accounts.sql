SET NAMES utf8mb4;

INSERT INTO `cdd_auth_account` (
  `id`, `user_id`, `account_name`, `display_name`, `mobile`, `email`, `password_hash`,
  `account_type`, `merchant_id`, `store_id`, `mini_program_id`, `status`, `last_login_at`,
  `token_version`, `created_by`, `updated_by`
)
SELECT
  9101001,
  'p_1001',
  'platform_admin',
  '平台管理员',
  NULL,
  NULL,
  '{noop}admin123456',
  'platform',
  NULL,
  NULL,
  NULL,
  'enabled',
  NULL,
  0,
  0,
  0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_account` WHERE `account_name` = 'platform_admin' AND `deleted` = 0
);

INSERT INTO `cdd_auth_account` (
  `id`, `user_id`, `account_name`, `display_name`, `mobile`, `email`, `password_hash`,
  `account_type`, `merchant_id`, `store_id`, `mini_program_id`, `status`, `last_login_at`,
  `token_version`, `created_by`, `updated_by`
)
SELECT
  9101002,
  'm_1001',
  'merchant_admin',
  '商家管理员',
  NULL,
  NULL,
  '{noop}merchant123456',
  'merchant',
  'merchant_1001',
  'store_1001',
  NULL,
  'enabled',
  NULL,
  0,
  0,
  0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_account` WHERE `account_name` = 'merchant_admin' AND `deleted` = 0
);

INSERT INTO `cdd_auth_account_role` (
  `id`, `account_id`, `role_id`, `created_by`, `updated_by`
)
SELECT 9101101, 9101001, 9200001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_account_role`
  WHERE `account_id` = 9101001 AND `role_id` = 9200001 AND `deleted` = 0
);

INSERT INTO `cdd_auth_account_role` (
  `id`, `account_id`, `role_id`, `created_by`, `updated_by`
)
SELECT 9101102, 9101002, 9200003, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_account_role`
  WHERE `account_id` = 9101002 AND `role_id` = 9200003 AND `deleted` = 0
);
