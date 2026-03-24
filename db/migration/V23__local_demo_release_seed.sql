SET NAMES utf8mb4;

UPDATE `cdd_merchant_mini_program`
SET
  `merchant_id` = 1001,
  `store_id` = 1001,
  `app_id` = 'wx-demo-1001',
  `app_secret_masked` = 'demo******1001',
  `payment_mch_id` = 'mch-demo-1001',
  `server_domain` = 'https://demo.chengdd.local',
  `binding_status` = 'active',
  `current_template_version` = '0.9.0',
  `last_detect_result_json` = JSON_OBJECT(
    'passed', TRUE,
    'issues', JSON_ARRAY(),
    'checked_at', '2026-03-24T10:00:00+08:00'
  ),
  `updated_by` = 1001,
  `deleted` = 0,
  `version` = 0
WHERE `id` = 1001;

INSERT INTO `cdd_merchant_mini_program` (
  `id`, `merchant_id`, `store_id`, `app_id`, `app_secret_masked`, `payment_mch_id`,
  `server_domain`, `binding_status`, `current_template_version`, `last_detect_result_json`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  1001, 1001, 1001, 'wx-demo-1001', 'demo******1001', 'mch-demo-1001',
  'https://demo.chengdd.local', 'active', '0.9.0',
  JSON_OBJECT(
    'passed', TRUE,
    'issues', JSON_ARRAY(),
    'checked_at', '2026-03-24T10:00:00+08:00'
  ),
  1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_merchant_mini_program` WHERE `id` = 1001 AND `deleted` = 0
);
