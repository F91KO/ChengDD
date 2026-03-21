SET NAMES utf8mb4;

SET @ddl_add_invalid_status = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_cart_item'
        AND COLUMN_NAME = 'invalid_status'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_cart_item`
       ADD COLUMN `invalid_status` varchar(32) NOT NULL DEFAULT ''valid'' COMMENT ''失效状态'' AFTER `selected`'
  )
);
PREPARE stmt_add_invalid_status FROM @ddl_add_invalid_status;
EXECUTE stmt_add_invalid_status;
DEALLOCATE PREPARE stmt_add_invalid_status;

SET @ddl_add_cart_index = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_cart_item'
        AND INDEX_NAME = 'idx_merchant_store_user_invalid_selected'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_cart_item`
       ADD KEY `idx_merchant_store_user_invalid_selected` (`merchant_id`, `store_id`, `user_id`, `invalid_status`, `selected`)'
  )
);
PREPARE stmt_add_cart_index FROM @ddl_add_cart_index;
EXECUTE stmt_add_cart_index;
DEALLOCATE PREPARE stmt_add_cart_index;

SET @ddl_add_pay_index = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_pay_record'
        AND INDEX_NAME = 'idx_order_id_pay_status'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_pay_record`
       ADD KEY `idx_order_id_pay_status` (`order_id`, `pay_status`)'
  )
);
PREPARE stmt_add_pay_index FROM @ddl_add_pay_index;
EXECUTE stmt_add_pay_index;
DEALLOCATE PREPARE stmt_add_pay_index;

SET @ddl_add_status_log_index = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_status_log'
        AND INDEX_NAME = 'idx_order_operate_created'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_status_log`
       ADD KEY `idx_order_operate_created` (`order_id`, `operate_type`, `created_at`)'
  )
);
PREPARE stmt_add_status_log_index FROM @ddl_add_status_log_index;
EXECUTE stmt_add_status_log_index;
DEALLOCATE PREPARE stmt_add_status_log_index;
