INSERT INTO cdd_template_version (
  id, template_code, template_name, template_version, template_type,
  status, created_by, updated_by
) VALUES
  (40001, 'tpl_shop', '商城模板', '1.0.0', 'wechat_mini_program', 'released', 0, 0),
  (40002, 'tpl_shop', '商城模板', '0.9.0', 'wechat_mini_program', 'deprecated', 0, 0);

INSERT INTO cdd_merchant_mini_program (
  id, merchant_id, store_id, app_id, app_secret_masked,
  payment_mch_id, server_domain, binding_status, current_template_version,
  last_detect_result_json, created_by, updated_by
) VALUES
  (30001, 10001, 20001, 'wx-test-30001', 'abc******xyz',
   'mch-test-001', 'test.example.com', 'active', NULL,
   '{"passed":true,"issues":[]}', 0, 0);
