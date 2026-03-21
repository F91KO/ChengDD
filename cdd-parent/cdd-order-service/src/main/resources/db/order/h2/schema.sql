CREATE TABLE IF NOT EXISTS cdd_order_cart_item (
  id BIGINT NOT NULL PRIMARY KEY,
  merchant_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  selected TINYINT NOT NULL DEFAULT 1,
  invalid_status VARCHAR(32) NOT NULL DEFAULT 'valid',
  snapshot_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
  created_by BIGINT DEFAULT NULL,
  updated_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_order_cart_user_store_sku_deleted
  ON cdd_order_cart_item (user_id, store_id, sku_id, deleted);
CREATE INDEX IF NOT EXISTS idx_order_cart_user_store_selected
  ON cdd_order_cart_item (user_id, store_id, selected);
CREATE INDEX IF NOT EXISTS idx_order_cart_merchant_user
  ON cdd_order_cart_item (merchant_id, user_id);

CREATE TABLE IF NOT EXISTS cdd_order_checkout_snapshot (
  id BIGINT NOT NULL PRIMARY KEY,
  merchant_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  snapshot_token VARCHAR(64) NOT NULL,
  cart_item_ids_json CLOB,
  pricing_snapshot_json CLOB NOT NULL,
  address_snapshot_json CLOB,
  coupon_snapshot_json CLOB,
  expired_at TIMESTAMP NOT NULL,
  created_by BIGINT DEFAULT NULL,
  updated_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_order_checkout_snapshot_token_deleted
  ON cdd_order_checkout_snapshot (snapshot_token, deleted);
CREATE INDEX IF NOT EXISTS idx_order_checkout_user_expired
  ON cdd_order_checkout_snapshot (user_id, expired_at);
CREATE INDEX IF NOT EXISTS idx_order_checkout_store_created
  ON cdd_order_checkout_snapshot (store_id, created_at);

CREATE TABLE IF NOT EXISTS cdd_order_info (
  id BIGINT NOT NULL PRIMARY KEY,
  order_no VARCHAR(32) NOT NULL,
  merchant_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  checkout_snapshot_id BIGINT DEFAULT NULL,
  order_status VARCHAR(32) NOT NULL,
  pay_status VARCHAR(32) NOT NULL,
  delivery_status VARCHAR(32) DEFAULT NULL,
  buyer_remark VARCHAR(512) DEFAULT NULL,
  total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
  discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
  payable_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
  paid_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
  delivery_fee_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
  receiver_name VARCHAR(64) DEFAULT NULL,
  receiver_mobile VARCHAR(32) DEFAULT NULL,
  receiver_address VARCHAR(512) DEFAULT NULL,
  paid_at TIMESTAMP DEFAULT NULL,
  cancelled_at TIMESTAMP DEFAULT NULL,
  finished_at TIMESTAMP DEFAULT NULL,
  created_by BIGINT DEFAULT NULL,
  updated_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_order_info_order_no_deleted
  ON cdd_order_info (order_no, deleted);
CREATE INDEX IF NOT EXISTS idx_order_info_user_created
  ON cdd_order_info (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_order_info_store_status_created
  ON cdd_order_info (store_id, order_status, created_at);
CREATE INDEX IF NOT EXISTS idx_order_info_store_pay_status
  ON cdd_order_info (store_id, pay_status);

CREATE TABLE IF NOT EXISTS cdd_order_item (
  id BIGINT NOT NULL PRIMARY KEY,
  order_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  product_name VARCHAR(256) NOT NULL,
  sku_name VARCHAR(256) NOT NULL,
  sku_spec_json CLOB,
  sale_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
  quantity INT NOT NULL DEFAULT 1,
  line_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
  refund_status VARCHAR(32) DEFAULT NULL,
  created_by BIGINT DEFAULT NULL,
  updated_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON cdd_order_item (order_id);
CREATE INDEX IF NOT EXISTS idx_order_item_sku_id ON cdd_order_item (sku_id);

CREATE TABLE IF NOT EXISTS cdd_order_pay_record (
  id BIGINT NOT NULL PRIMARY KEY,
  pay_no VARCHAR(32) NOT NULL,
  order_id BIGINT NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  merchant_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  pay_channel VARCHAR(32) NOT NULL,
  pay_method VARCHAR(32) DEFAULT NULL,
  pay_status VARCHAR(32) NOT NULL,
  pay_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
  third_party_trade_no VARCHAR(64) DEFAULT NULL,
  pay_request_json CLOB,
  pay_response_json CLOB,
  paid_at TIMESTAMP DEFAULT NULL,
  created_by BIGINT DEFAULT NULL,
  updated_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_order_pay_pay_no_deleted
  ON cdd_order_pay_record (pay_no, deleted);
CREATE UNIQUE INDEX IF NOT EXISTS uk_order_pay_trade_no_deleted
  ON cdd_order_pay_record (third_party_trade_no, deleted);
CREATE INDEX IF NOT EXISTS idx_order_pay_order_id
  ON cdd_order_pay_record (order_id);

CREATE TABLE IF NOT EXISTS cdd_order_pay_callback_record (
  id BIGINT NOT NULL PRIMARY KEY,
  pay_record_id BIGINT DEFAULT NULL,
  pay_no VARCHAR(32) NOT NULL,
  merchant_id BIGINT NOT NULL,
  store_id BIGINT DEFAULT NULL,
  pay_channel VARCHAR(32) NOT NULL,
  third_party_trade_no VARCHAR(64) DEFAULT NULL,
  callback_event_id VARCHAR(64) NOT NULL,
  callback_status VARCHAR(32) NOT NULL,
  processed_result VARCHAR(32) DEFAULT NULL,
  failure_reason VARCHAR(512) DEFAULT NULL,
  callback_payload_json CLOB,
  processed_at TIMESTAMP DEFAULT NULL,
  created_by BIGINT DEFAULT NULL,
  updated_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_order_pay_callback_event_deleted
  ON cdd_order_pay_callback_record (pay_no, callback_event_id, deleted);
CREATE INDEX IF NOT EXISTS idx_order_pay_callback_pay_record_id
  ON cdd_order_pay_callback_record (pay_record_id);

CREATE TABLE IF NOT EXISTS cdd_order_status_log (
  id BIGINT NOT NULL PRIMARY KEY,
  order_id BIGINT NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  from_status VARCHAR(32) DEFAULT NULL,
  to_status VARCHAR(32) NOT NULL,
  operate_type VARCHAR(32) NOT NULL,
  operator_id BIGINT DEFAULT NULL,
  operator_name VARCHAR(64) DEFAULT NULL,
  remark VARCHAR(512) DEFAULT NULL,
  created_by BIGINT DEFAULT NULL,
  updated_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_order_status_log_order_id
  ON cdd_order_status_log (order_id);
CREATE INDEX IF NOT EXISTS idx_order_status_log_order_created
  ON cdd_order_status_log (order_no, created_at);

CREATE TABLE IF NOT EXISTS cdd_order_refund_record (
  id BIGINT NOT NULL PRIMARY KEY,
  refund_no VARCHAR(32) NOT NULL,
  order_id BIGINT NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  pay_record_id BIGINT DEFAULT NULL,
  merchant_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  refund_reason VARCHAR(512) DEFAULT NULL,
  refund_status VARCHAR(32) NOT NULL,
  refund_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
  third_party_refund_no VARCHAR(64) DEFAULT NULL,
  applied_at TIMESTAMP NOT NULL,
  success_at TIMESTAMP DEFAULT NULL,
  failure_reason VARCHAR(512) DEFAULT NULL,
  compensation_task_code VARCHAR(64) DEFAULT NULL,
  created_by BIGINT DEFAULT NULL,
  updated_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_order_refund_no_deleted
  ON cdd_order_refund_record (refund_no, deleted);
CREATE INDEX IF NOT EXISTS idx_order_refund_order_id
  ON cdd_order_refund_record (order_id);

CREATE TABLE IF NOT EXISTS cdd_order_refund_callback_record (
  id BIGINT NOT NULL PRIMARY KEY,
  refund_record_id BIGINT DEFAULT NULL,
  refund_no VARCHAR(32) NOT NULL,
  order_id BIGINT DEFAULT NULL,
  merchant_id BIGINT NOT NULL,
  store_id BIGINT DEFAULT NULL,
  third_party_refund_no VARCHAR(64) DEFAULT NULL,
  callback_event_id VARCHAR(64) NOT NULL,
  callback_status VARCHAR(32) NOT NULL,
  processed_result VARCHAR(32) DEFAULT NULL,
  failure_reason VARCHAR(512) DEFAULT NULL,
  callback_payload_json CLOB,
  processed_at TIMESTAMP DEFAULT NULL,
  created_by BIGINT DEFAULT NULL,
  updated_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_order_refund_callback_event_deleted
  ON cdd_order_refund_callback_record (refund_no, callback_event_id, deleted);
CREATE INDEX IF NOT EXISTS idx_order_refund_callback_refund_record_id
  ON cdd_order_refund_callback_record (refund_record_id);

CREATE TABLE IF NOT EXISTS cdd_compensation_task (
  id BIGINT NOT NULL PRIMARY KEY,
  task_code VARCHAR(64) NOT NULL,
  biz_type VARCHAR(64) NOT NULL,
  biz_id VARCHAR(64) NOT NULL,
  compensation_type VARCHAR(64) NOT NULL,
  task_status VARCHAR(32) NOT NULL,
  retry_count INT NOT NULL DEFAULT 0,
  max_retry_count INT NOT NULL DEFAULT 10,
  next_retry_at TIMESTAMP DEFAULT NULL,
  last_error_code VARCHAR(64) DEFAULT NULL,
  last_error_message VARCHAR(512) DEFAULT NULL,
  payload_json CLOB,
  resolved_at TIMESTAMP DEFAULT NULL,
  created_by BIGINT DEFAULT NULL,
  updated_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_compensation_task_code_deleted
  ON cdd_compensation_task (task_code, deleted);
CREATE UNIQUE INDEX IF NOT EXISTS uk_compensation_biz_deleted
  ON cdd_compensation_task (biz_type, biz_id, compensation_type, deleted);
