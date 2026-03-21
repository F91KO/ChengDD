MERGE INTO cdd_auth_role (id, role_code, role_name, role_type, status, created_by, updated_by, deleted, version)
KEY(id) VALUES
  (9200001, 'platform_admin', '平台管理员', 'platform', 'enabled', 0, 0, 0, 0),
  (9200003, 'merchant_owner', '商家主账号', 'merchant', 'enabled', 0, 0, 0, 0);

MERGE INTO cdd_auth_account (
  id, user_id, account_name, display_name, password_hash, account_type,
  merchant_id, store_id, mini_program_id, token_version, status, created_by, updated_by, deleted, version
)
KEY(id) VALUES
  (9101001, 'p_1001', 'platform_admin', '平台管理员', '{noop}admin123456', 'platform', NULL, NULL, NULL, 0, 'enabled', 0, 0, 0, 0),
  (9101002, 'm_1001', 'merchant_admin', '商家管理员', '{noop}merchant123456', 'merchant', 'merchant_1001', 'store_1001', NULL, 0, 'enabled', 0, 0, 0, 0);

MERGE INTO cdd_auth_account_role (id, account_id, role_id, created_by, updated_by, deleted, version)
KEY(id) VALUES
  (9500001, 9101001, 9200001, 0, 0, 0, 0),
  (9500002, 9101002, 9200003, 0, 0, 0, 0);
