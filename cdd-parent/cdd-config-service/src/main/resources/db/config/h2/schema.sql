CREATE TABLE IF NOT EXISTS cdd_config_kv (
  id BIGINT NOT NULL PRIMARY KEY,
  config_group VARCHAR(64) NOT NULL,
  config_key VARCHAR(128) NOT NULL,
  config_value CLOB,
  config_desc VARCHAR(512),
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_group_key_deleted
  ON cdd_config_kv (config_group, config_key, deleted);

CREATE TABLE IF NOT EXISTS cdd_config_kv_merchant_override (
  id BIGINT NOT NULL PRIMARY KEY,
  merchant_id VARCHAR(64) NOT NULL,
  config_group VARCHAR(64) NOT NULL,
  config_key VARCHAR(128) NOT NULL,
  config_value CLOB,
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_merchant_group_key_deleted
  ON cdd_config_kv_merchant_override (merchant_id, config_group, config_key, deleted);

CREATE TABLE IF NOT EXISTS cdd_config_feature_switch (
  id BIGINT NOT NULL PRIMARY KEY,
  switch_code VARCHAR(64) NOT NULL,
  switch_name VARCHAR(128) NOT NULL,
  switch_scope VARCHAR(32) NOT NULL,
  default_value VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_switch_code_deleted
  ON cdd_config_feature_switch (switch_code, deleted);

CREATE TABLE IF NOT EXISTS cdd_config_feature_switch_merchant_override (
  id BIGINT NOT NULL PRIMARY KEY,
  switch_id BIGINT NOT NULL,
  merchant_id VARCHAR(64) NOT NULL,
  switch_value VARCHAR(32) NOT NULL,
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_switch_merchant_deleted
  ON cdd_config_feature_switch_merchant_override (switch_id, merchant_id, deleted);
