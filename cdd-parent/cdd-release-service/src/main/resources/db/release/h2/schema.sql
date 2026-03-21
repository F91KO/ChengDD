CREATE TABLE IF NOT EXISTS cdd_template_version (
  id BIGINT PRIMARY KEY,
  template_code VARCHAR(64) NOT NULL,
  template_name VARCHAR(128) NOT NULL,
  template_version VARCHAR(32) NOT NULL,
  template_type VARCHAR(32) NOT NULL,
  runtime_package_url VARCHAR(512),
  source_package_url VARCHAR(512),
  change_summary VARCHAR(1024),
  status VARCHAR(32) NOT NULL,
  released_at TIMESTAMP,
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_template_version_deleted
  ON cdd_template_version (template_code, template_version, deleted);

CREATE TABLE IF NOT EXISTS cdd_merchant_mini_program (
  id BIGINT PRIMARY KEY,
  merchant_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  app_id VARCHAR(64) NOT NULL,
  app_secret_masked VARCHAR(128),
  payment_mch_id VARCHAR(64),
  server_domain VARCHAR(256),
  binding_status VARCHAR(32) NOT NULL,
  current_template_version VARCHAR(64),
  last_detect_result_json VARCHAR(2048),
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_app_id_deleted
  ON cdd_merchant_mini_program (app_id, deleted);

CREATE TABLE IF NOT EXISTS cdd_release_task (
  id BIGINT PRIMARY KEY,
  task_no VARCHAR(32) NOT NULL,
  merchant_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  mini_program_id BIGINT NOT NULL,
  template_version_id BIGINT NOT NULL,
  release_type VARCHAR(32) NOT NULL,
  release_status VARCHAR(32) NOT NULL,
  current_step_code VARCHAR(64),
  result_sync_status VARCHAR(32) NOT NULL DEFAULT 'pending',
  rollback_task_no VARCHAR(32),
  last_error_code VARCHAR(64),
  last_error_message VARCHAR(512),
  trigger_source VARCHAR(32) NOT NULL,
  release_snapshot_json VARCHAR(4000),
  started_at TIMESTAMP,
  finished_at TIMESTAMP,
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_task_no_deleted
  ON cdd_release_task (task_no, deleted);

CREATE INDEX IF NOT EXISTS idx_release_status_sync
  ON cdd_release_task (release_status, result_sync_status);

CREATE INDEX IF NOT EXISTS idx_mini_program_created_at
  ON cdd_release_task (mini_program_id, created_at);

CREATE TABLE IF NOT EXISTS cdd_release_task_detail (
  id BIGINT PRIMARY KEY,
  task_id BIGINT NOT NULL,
  step_code VARCHAR(64) NOT NULL,
  step_name VARCHAR(128) NOT NULL,
  step_status VARCHAR(32) NOT NULL,
  step_order INT NOT NULL DEFAULT 0,
  result_message VARCHAR(512),
  error_code VARCHAR(64),
  retry_count INT NOT NULL DEFAULT 0,
  started_at TIMESTAMP,
  finished_at TIMESTAMP,
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_task_step_code_deleted
  ON cdd_release_task_detail (task_id, step_code, deleted);

CREATE INDEX IF NOT EXISTS idx_task_step_order
  ON cdd_release_task_detail (task_id, step_order);

CREATE INDEX IF NOT EXISTS idx_task_step_status
  ON cdd_release_task_detail (task_id, step_status);

CREATE TABLE IF NOT EXISTS cdd_release_rollback_record (
  id BIGINT PRIMARY KEY,
  task_id BIGINT NOT NULL,
  rollback_target_version VARCHAR(32) NOT NULL,
  rollback_reason VARCHAR(512),
  rollback_status VARCHAR(32) NOT NULL,
  rolled_back_at TIMESTAMP,
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_release_rollback_task_id
  ON cdd_release_rollback_record (task_id);

CREATE TABLE IF NOT EXISTS cdd_release_log (
  id BIGINT PRIMARY KEY,
  task_id BIGINT NOT NULL,
  log_level VARCHAR(16) NOT NULL,
  log_stage VARCHAR(64),
  log_content CLOB,
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_task_created_at
  ON cdd_release_log (task_id, created_at);

CREATE TABLE IF NOT EXISTS cdd_release_version_mapping (
  id BIGINT PRIMARY KEY,
  merchant_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  mini_program_id BIGINT NOT NULL,
  template_version_id BIGINT NOT NULL,
  template_code VARCHAR(64) NOT NULL,
  template_version VARCHAR(32) NOT NULL,
  mapping_status VARCHAR(32) NOT NULL,
  source_task_id BIGINT NOT NULL,
  activated_at TIMESTAMP NOT NULL,
  deactivated_at TIMESTAMP,
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mini_program_template_deleted
  ON cdd_release_version_mapping (mini_program_id, template_version_id, deleted);

CREATE INDEX IF NOT EXISTS idx_mini_program_mapping_status
  ON cdd_release_version_mapping (mini_program_id, mapping_status);

CREATE INDEX IF NOT EXISTS idx_source_task_id
  ON cdd_release_version_mapping (source_task_id);
