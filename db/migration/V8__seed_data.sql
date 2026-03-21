SET NAMES utf8mb4;

INSERT INTO `cdd_config_id_segment` (
  `id`, `biz_code`, `current_max_id`, `step_size`, `segment_desc`, `created_by`, `updated_by`
)
SELECT 9100001, 'merchant', 1000000, 1000, '商家主体发号', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_config_id_segment` WHERE `biz_code` = 'merchant' AND `deleted` = 0
);

INSERT INTO `cdd_config_id_segment` (
  `id`, `biz_code`, `current_max_id`, `step_size`, `segment_desc`, `created_by`, `updated_by`
)
SELECT 9100002, 'store', 2000000, 1000, '店铺发号', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_config_id_segment` WHERE `biz_code` = 'store' AND `deleted` = 0
);

INSERT INTO `cdd_config_id_segment` (
  `id`, `biz_code`, `current_max_id`, `step_size`, `segment_desc`, `created_by`, `updated_by`
)
SELECT 9100003, 'product', 3000000, 1000, '商品发号', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_config_id_segment` WHERE `biz_code` = 'product' AND `deleted` = 0
);

INSERT INTO `cdd_config_id_segment` (
  `id`, `biz_code`, `current_max_id`, `step_size`, `segment_desc`, `created_by`, `updated_by`
)
SELECT 9100004, 'order', 4000000, 1000, '订单发号', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_config_id_segment` WHERE `biz_code` = 'order' AND `deleted` = 0
);

INSERT INTO `cdd_config_id_segment` (
  `id`, `biz_code`, `current_max_id`, `step_size`, `segment_desc`, `created_by`, `updated_by`
)
SELECT 9100005, 'release_task', 5000000, 1000, '发布任务发号', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_config_id_segment` WHERE `biz_code` = 'release_task' AND `deleted` = 0
);

INSERT INTO `cdd_auth_role` (
  `id`, `role_code`, `role_name`, `role_type`, `status`, `created_by`, `updated_by`
)
SELECT 9200001, 'platform_admin', '平台管理员', 'platform', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role` WHERE `role_code` = 'platform_admin' AND `deleted` = 0
);

INSERT INTO `cdd_auth_role` (
  `id`, `role_code`, `role_name`, `role_type`, `status`, `created_by`, `updated_by`
)
SELECT 9200002, 'delivery_admin', '交付管理员', 'platform', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role` WHERE `role_code` = 'delivery_admin' AND `deleted` = 0
);

INSERT INTO `cdd_auth_role` (
  `id`, `role_code`, `role_name`, `role_type`, `status`, `created_by`, `updated_by`
)
SELECT 9200003, 'merchant_owner', '商家主账号', 'merchant', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role` WHERE `role_code` = 'merchant_owner' AND `deleted` = 0
);

INSERT INTO `cdd_auth_permission` (
  `id`, `permission_code`, `permission_name`, `permission_group`, `status`, `created_by`, `updated_by`
)
SELECT 9300001, 'platform.dashboard.view', '查看平台首页', 'platform_dashboard', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_permission` WHERE `permission_code` = 'platform.dashboard.view' AND `deleted` = 0
);

INSERT INTO `cdd_auth_permission` (
  `id`, `permission_code`, `permission_name`, `permission_group`, `status`, `created_by`, `updated_by`
)
SELECT 9300002, 'platform.merchant.review', '审核商家入驻', 'platform_merchant', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_permission` WHERE `permission_code` = 'platform.merchant.review' AND `deleted` = 0
);

INSERT INTO `cdd_auth_permission` (
  `id`, `permission_code`, `permission_name`, `permission_group`, `status`, `created_by`, `updated_by`
)
SELECT 9300003, 'platform.template.manage', '管理模板版本', 'platform_release', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_permission` WHERE `permission_code` = 'platform.template.manage' AND `deleted` = 0
);

INSERT INTO `cdd_auth_permission` (
  `id`, `permission_code`, `permission_name`, `permission_group`, `status`, `created_by`, `updated_by`
)
SELECT 9300004, 'platform.release.manage', '管理发布任务', 'platform_release', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_permission` WHERE `permission_code` = 'platform.release.manage' AND `deleted` = 0
);

INSERT INTO `cdd_auth_permission` (
  `id`, `permission_code`, `permission_name`, `permission_group`, `status`, `created_by`, `updated_by`
)
SELECT 9300005, 'merchant.store.manage', '管理店铺资料', 'merchant_store', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_permission` WHERE `permission_code` = 'merchant.store.manage' AND `deleted` = 0
);

INSERT INTO `cdd_auth_permission` (
  `id`, `permission_code`, `permission_name`, `permission_group`, `status`, `created_by`, `updated_by`
)
SELECT 9300006, 'merchant.product.manage', '管理商品分类商品', 'merchant_product', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_permission` WHERE `permission_code` = 'merchant.product.manage' AND `deleted` = 0
);

INSERT INTO `cdd_auth_permission` (
  `id`, `permission_code`, `permission_name`, `permission_group`, `status`, `created_by`, `updated_by`
)
SELECT 9300007, 'merchant.order.manage', '管理订单售后', 'merchant_order', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_permission` WHERE `permission_code` = 'merchant.order.manage' AND `deleted` = 0
);

INSERT INTO `cdd_auth_permission` (
  `id`, `permission_code`, `permission_name`, `permission_group`, `status`, `created_by`, `updated_by`
)
SELECT 9300008, 'merchant.release.publish', '执行开通发布', 'merchant_release', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_permission` WHERE `permission_code` = 'merchant.release.publish' AND `deleted` = 0
);

INSERT INTO `cdd_auth_role_permission` (
  `id`, `role_id`, `permission_id`, `created_by`, `updated_by`
)
SELECT 9400001, 9200001, 9300001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role_permission` WHERE `role_id` = 9200001 AND `permission_id` = 9300001 AND `deleted` = 0
);

INSERT INTO `cdd_auth_role_permission` (
  `id`, `role_id`, `permission_id`, `created_by`, `updated_by`
)
SELECT 9400002, 9200001, 9300002, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role_permission` WHERE `role_id` = 9200001 AND `permission_id` = 9300002 AND `deleted` = 0
);

INSERT INTO `cdd_auth_role_permission` (
  `id`, `role_id`, `permission_id`, `created_by`, `updated_by`
)
SELECT 9400003, 9200001, 9300003, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role_permission` WHERE `role_id` = 9200001 AND `permission_id` = 9300003 AND `deleted` = 0
);

INSERT INTO `cdd_auth_role_permission` (
  `id`, `role_id`, `permission_id`, `created_by`, `updated_by`
)
SELECT 9400004, 9200001, 9300004, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role_permission` WHERE `role_id` = 9200001 AND `permission_id` = 9300004 AND `deleted` = 0
);

INSERT INTO `cdd_auth_role_permission` (
  `id`, `role_id`, `permission_id`, `created_by`, `updated_by`
)
SELECT 9400005, 9200002, 9300002, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role_permission` WHERE `role_id` = 9200002 AND `permission_id` = 9300002 AND `deleted` = 0
);

INSERT INTO `cdd_auth_role_permission` (
  `id`, `role_id`, `permission_id`, `created_by`, `updated_by`
)
SELECT 9400006, 9200002, 9300003, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role_permission` WHERE `role_id` = 9200002 AND `permission_id` = 9300003 AND `deleted` = 0
);

INSERT INTO `cdd_auth_role_permission` (
  `id`, `role_id`, `permission_id`, `created_by`, `updated_by`
)
SELECT 9400007, 9200002, 9300004, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role_permission` WHERE `role_id` = 9200002 AND `permission_id` = 9300004 AND `deleted` = 0
);

INSERT INTO `cdd_auth_role_permission` (
  `id`, `role_id`, `permission_id`, `created_by`, `updated_by`
)
SELECT 9400008, 9200003, 9300005, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role_permission` WHERE `role_id` = 9200003 AND `permission_id` = 9300005 AND `deleted` = 0
);

INSERT INTO `cdd_auth_role_permission` (
  `id`, `role_id`, `permission_id`, `created_by`, `updated_by`
)
SELECT 9400009, 9200003, 9300006, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role_permission` WHERE `role_id` = 9200003 AND `permission_id` = 9300006 AND `deleted` = 0
);

INSERT INTO `cdd_auth_role_permission` (
  `id`, `role_id`, `permission_id`, `created_by`, `updated_by`
)
SELECT 9400010, 9200003, 9300007, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role_permission` WHERE `role_id` = 9200003 AND `permission_id` = 9300007 AND `deleted` = 0
);

INSERT INTO `cdd_auth_role_permission` (
  `id`, `role_id`, `permission_id`, `created_by`, `updated_by`
)
SELECT 9400011, 9200003, 9300008, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_auth_role_permission` WHERE `role_id` = 9200003 AND `permission_id` = 9300008 AND `deleted` = 0
);

INSERT INTO `cdd_config_feature_switch` (
  `id`, `switch_code`, `switch_name`, `switch_scope`, `default_value`, `status`, `created_by`, `updated_by`
)
SELECT 9500001, 'merchant_self_onboarding', '商家自助开通', 'merchant', 'on', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_config_feature_switch` WHERE `switch_code` = 'merchant_self_onboarding' AND `deleted` = 0
);

INSERT INTO `cdd_config_feature_switch` (
  `id`, `switch_code`, `switch_name`, `switch_scope`, `default_value`, `status`, `created_by`, `updated_by`
)
SELECT 9500002, 'product_batch_publish', '商品批量发布', 'store', 'on', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_config_feature_switch` WHERE `switch_code` = 'product_batch_publish' AND `deleted` = 0
);

INSERT INTO `cdd_config_feature_switch` (
  `id`, `switch_code`, `switch_name`, `switch_scope`, `default_value`, `status`, `created_by`, `updated_by`
)
SELECT 9500003, 'gray_release', '灰度发布能力', 'platform', 'off', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_config_feature_switch` WHERE `switch_code` = 'gray_release' AND `deleted` = 0
);

INSERT INTO `cdd_config_feature_switch` (
  `id`, `switch_code`, `switch_name`, `switch_scope`, `default_value`, `status`, `created_by`, `updated_by`
)
SELECT 9500004, 'after_sale_enabled', '售后能力开关', 'store', 'on', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_config_feature_switch` WHERE `switch_code` = 'after_sale_enabled' AND `deleted` = 0
);

INSERT INTO `cdd_config_preset_rule` (
  `id`, `rule_code`, `rule_name`, `rule_type`, `rule_payload_json`, `status`, `created_by`, `updated_by`
)
SELECT 9600001, 'home_template.default.fresh', '生鲜首页默认预置', 'home_template', '{"schema_version":1,"template_code":"home_fresh_default","style_mode":"fresh"}', 'enabled', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_config_preset_rule` WHERE `rule_code` = 'home_template.default.fresh' AND `deleted` = 0
);

INSERT INTO `cdd_config_kv` (
  `id`, `config_group`, `config_key`, `config_value`, `config_desc`, `created_by`, `updated_by`
)
SELECT 9700001, 'system', 'default_time_zone', 'Asia/Shanghai', '系统默认时区', 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_config_kv` WHERE `config_group` = 'system' AND `config_key` = 'default_time_zone' AND `deleted` = 0
);

INSERT INTO `cdd_template_version` (
  `id`, `template_code`, `template_name`, `template_version`, `template_type`, `runtime_package_url`,
  `source_package_url`, `change_summary`, `status`, `released_at`, `created_by`, `updated_by`
)
SELECT
  9800001,
  'home_fresh_default',
  '生鲜首页默认模板',
  '1.0.0',
  'mini_program',
  NULL,
  NULL,
  '初始化默认模板版本',
  'released',
  CURRENT_TIMESTAMP,
  0,
  0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_template_version`
  WHERE `template_code` = 'home_fresh_default' AND `template_version` = '1.0.0' AND `deleted` = 0
);
