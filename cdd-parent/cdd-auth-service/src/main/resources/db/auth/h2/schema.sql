CREATE TABLE IF NOT EXISTS cdd_auth_account (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  account_name VARCHAR(64) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  mobile VARCHAR(32),
  email VARCHAR(128),
  password_hash VARCHAR(256),
  account_type VARCHAR(32) NOT NULL,
  merchant_id VARCHAR(64),
  store_id VARCHAR(64),
  mini_program_id VARCHAR(64),
  token_version BIGINT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL,
  last_login_at TIMESTAMP NULL,
  created_by BIGINT DEFAULT NULL,
  updated_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_account_user_id_deleted ON cdd_auth_account (user_id, deleted);
CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_account_name_deleted ON cdd_auth_account (account_name, deleted);
CREATE INDEX IF NOT EXISTS idx_auth_account_type_status ON cdd_auth_account (account_type, status);

CREATE TABLE IF NOT EXISTS cdd_auth_role (
  id BIGINT NOT NULL PRIMARY KEY,
  role_code VARCHAR(64) NOT NULL,
  role_name VARCHAR(128) NOT NULL,
  role_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_by BIGINT DEFAULT NULL,
  updated_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_role_code_deleted ON cdd_auth_role (role_code, deleted);

CREATE TABLE IF NOT EXISTS cdd_auth_account_role (
  id BIGINT NOT NULL PRIMARY KEY,
  account_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_by BIGINT DEFAULT NULL,
  updated_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_account_role_deleted ON cdd_auth_account_role (account_id, role_id, deleted);
CREATE INDEX IF NOT EXISTS idx_auth_account_role_account_id ON cdd_auth_account_role (account_id);

CREATE TABLE IF NOT EXISTS cdd_auth_refresh_token_session (
  id BIGINT NOT NULL PRIMARY KEY,
  token_id VARCHAR(64) NOT NULL,
  account_id BIGINT NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  token_hash VARCHAR(128) NOT NULL,
  token_version BIGINT NOT NULL DEFAULT 0,
  expires_at TIMESTAMP NOT NULL,
  revoked_at TIMESTAMP NULL,
  created_by BIGINT DEFAULT NULL,
  updated_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_refresh_token_deleted ON cdd_auth_refresh_token_session (token_id, deleted);
CREATE INDEX IF NOT EXISTS idx_auth_refresh_user_deleted ON cdd_auth_refresh_token_session (user_id, deleted);
