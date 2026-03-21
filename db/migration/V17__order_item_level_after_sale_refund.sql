SET NAMES utf8mb4;

SET @ddl_add_order_item_refund_status = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_item'
        AND COLUMN_NAME = 'refund_status'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_item`
       ADD COLUMN `refund_status` varchar(32) NOT NULL DEFAULT ''none'' COMMENT ''退款状态'' AFTER `line_amount`'
  )
);
PREPARE stmt_add_order_item_refund_status FROM @ddl_add_order_item_refund_status;
EXECUTE stmt_add_order_item_refund_status;
DEALLOCATE PREPARE stmt_add_order_item_refund_status;

SET @ddl_modify_order_item_refund_status = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_item'
        AND COLUMN_NAME = 'refund_status'
        AND IS_NULLABLE = 'NO'
        AND COLUMN_DEFAULT = 'none'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_item`
       MODIFY COLUMN `refund_status` varchar(32) NOT NULL DEFAULT ''none'' COMMENT ''退款状态'''
  )
);
PREPARE stmt_modify_order_item_refund_status FROM @ddl_modify_order_item_refund_status;
EXECUTE stmt_modify_order_item_refund_status;
DEALLOCATE PREPARE stmt_modify_order_item_refund_status;

SET @ddl_add_order_item_refunded_quantity = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_item'
        AND COLUMN_NAME = 'refunded_quantity'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_item`
       ADD COLUMN `refunded_quantity` int NOT NULL DEFAULT 0 COMMENT ''累计退款件数'' AFTER `refund_status`'
  )
);
PREPARE stmt_add_order_item_refunded_quantity FROM @ddl_add_order_item_refunded_quantity;
EXECUTE stmt_add_order_item_refunded_quantity;
DEALLOCATE PREPARE stmt_add_order_item_refunded_quantity;

SET @ddl_add_order_item_refunded_amount = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_item'
        AND COLUMN_NAME = 'refunded_amount'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_item`
       ADD COLUMN `refunded_amount` decimal(10,2) NOT NULL DEFAULT ''0.00'' COMMENT ''累计退款金额'' AFTER `refunded_quantity`'
  )
);
PREPARE stmt_add_order_item_refunded_amount FROM @ddl_add_order_item_refunded_amount;
EXECUTE stmt_add_order_item_refunded_amount;
DEALLOCATE PREPARE stmt_add_order_item_refunded_amount;

SET @ddl_add_refund_record_failure_reason = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_refund_record'
        AND COLUMN_NAME = 'failure_reason'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_refund_record`
       ADD COLUMN `failure_reason` varchar(512) DEFAULT NULL COMMENT ''失败原因'' AFTER `success_at`'
  )
);
PREPARE stmt_add_refund_record_failure_reason FROM @ddl_add_refund_record_failure_reason;
EXECUTE stmt_add_refund_record_failure_reason;
DEALLOCATE PREPARE stmt_add_refund_record_failure_reason;

SET @ddl_add_refund_record_compensation_task_code = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_refund_record'
        AND COLUMN_NAME = 'compensation_task_code'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_refund_record`
       ADD COLUMN `compensation_task_code` varchar(64) DEFAULT NULL COMMENT ''补偿任务编码'' AFTER `failure_reason`'
  )
);
PREPARE stmt_add_refund_record_compensation_task_code FROM @ddl_add_refund_record_compensation_task_code;
EXECUTE stmt_add_refund_record_compensation_task_code;
DEALLOCATE PREPARE stmt_add_refund_record_compensation_task_code;

SET @ddl_add_refund_record_after_sale_id = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_refund_record'
        AND COLUMN_NAME = 'after_sale_id'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_refund_record`
       ADD COLUMN `after_sale_id` bigint DEFAULT NULL COMMENT ''售后单ID'' AFTER `store_id`'
  )
);
PREPARE stmt_add_refund_record_after_sale_id FROM @ddl_add_refund_record_after_sale_id;
EXECUTE stmt_add_refund_record_after_sale_id;
DEALLOCATE PREPARE stmt_add_refund_record_after_sale_id;

SET @ddl_add_refund_record_order_item_id = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_refund_record'
        AND COLUMN_NAME = 'order_item_id'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_refund_record`
       ADD COLUMN `order_item_id` bigint DEFAULT NULL COMMENT ''订单项ID'' AFTER `order_no`'
  )
);
PREPARE stmt_add_refund_record_order_item_id FROM @ddl_add_refund_record_order_item_id;
EXECUTE stmt_add_refund_record_order_item_id;
DEALLOCATE PREPARE stmt_add_refund_record_order_item_id;

SET @ddl_add_refund_record_refund_quantity = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_refund_record'
        AND COLUMN_NAME = 'refund_quantity'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_refund_record`
       ADD COLUMN `refund_quantity` int NOT NULL DEFAULT 0 COMMENT ''退款件数'' AFTER `refund_amount`'
  )
);
PREPARE stmt_add_refund_record_refund_quantity FROM @ddl_add_refund_record_refund_quantity;
EXECUTE stmt_add_refund_record_refund_quantity;
DEALLOCATE PREPARE stmt_add_refund_record_refund_quantity;

SET @ddl_add_refund_record_after_sale_index = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_refund_record'
        AND INDEX_NAME = 'idx_after_sale_id'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_refund_record`
       ADD KEY `idx_after_sale_id` (`after_sale_id`)'
  )
);
PREPARE stmt_add_refund_record_after_sale_index FROM @ddl_add_refund_record_after_sale_index;
EXECUTE stmt_add_refund_record_after_sale_index;
DEALLOCATE PREPARE stmt_add_refund_record_after_sale_index;

SET @ddl_add_refund_record_item_status_index = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_refund_record'
        AND INDEX_NAME = 'idx_order_item_refund_status'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_refund_record`
       ADD KEY `idx_order_item_refund_status` (`order_item_id`, `refund_status`)'
  )
);
PREPARE stmt_add_refund_record_item_status_index FROM @ddl_add_refund_record_item_status_index;
EXECUTE stmt_add_refund_record_item_status_index;
DEALLOCATE PREPARE stmt_add_refund_record_item_status_index;

SET @ddl_add_after_sale_order_no = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_after_sale'
        AND COLUMN_NAME = 'order_no'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_after_sale`
       ADD COLUMN `order_no` varchar(32) NOT NULL DEFAULT '''' COMMENT ''订单号'' AFTER `order_id`'
  )
);
PREPARE stmt_add_after_sale_order_no FROM @ddl_add_after_sale_order_no;
EXECUTE stmt_add_after_sale_order_no;
DEALLOCATE PREPARE stmt_add_after_sale_order_no;

SET @ddl_add_after_sale_refund_record_id = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_after_sale'
        AND COLUMN_NAME = 'refund_record_id'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_after_sale`
       ADD COLUMN `refund_record_id` bigint DEFAULT NULL COMMENT ''退款记录ID'' AFTER `after_sale_status`'
  )
);
PREPARE stmt_add_after_sale_refund_record_id FROM @ddl_add_after_sale_refund_record_id;
EXECUTE stmt_add_after_sale_refund_record_id;
DEALLOCATE PREPARE stmt_add_after_sale_refund_record_id;

SET @ddl_add_after_sale_refund_no = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_after_sale'
        AND COLUMN_NAME = 'refund_no'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_after_sale`
       ADD COLUMN `refund_no` varchar(32) DEFAULT NULL COMMENT ''退款单号'' AFTER `refund_record_id`'
  )
);
PREPARE stmt_add_after_sale_refund_no FROM @ddl_add_after_sale_refund_no;
EXECUTE stmt_add_after_sale_refund_no;
DEALLOCATE PREPARE stmt_add_after_sale_refund_no;

SET @ddl_add_after_sale_refund_quantity = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_after_sale'
        AND COLUMN_NAME = 'refund_quantity'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_after_sale`
       ADD COLUMN `refund_quantity` int NOT NULL DEFAULT 0 COMMENT ''退款件数'' AFTER `refund_amount`'
  )
);
PREPARE stmt_add_after_sale_refund_quantity FROM @ddl_add_after_sale_refund_quantity;
EXECUTE stmt_add_after_sale_refund_quantity;
DEALLOCATE PREPARE stmt_add_after_sale_refund_quantity;

SET @ddl_add_after_sale_reason_code = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_after_sale'
        AND COLUMN_NAME = 'reason_code'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_after_sale`
       ADD COLUMN `reason_code` varchar(64) DEFAULT NULL COMMENT ''售后原因编码'' AFTER `apply_desc`'
  )
);
PREPARE stmt_add_after_sale_reason_code FROM @ddl_add_after_sale_reason_code;
EXECUTE stmt_add_after_sale_reason_code;
DEALLOCATE PREPARE stmt_add_after_sale_reason_code;

SET @ddl_add_after_sale_reason_desc = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_after_sale'
        AND COLUMN_NAME = 'reason_desc'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_after_sale`
       ADD COLUMN `reason_desc` varchar(512) DEFAULT NULL COMMENT ''售后原因说明'' AFTER `reason_code`'
  )
);
PREPARE stmt_add_after_sale_reason_desc FROM @ddl_add_after_sale_reason_desc;
EXECUTE stmt_add_after_sale_reason_desc;
DEALLOCATE PREPARE stmt_add_after_sale_reason_desc;

SET @ddl_add_after_sale_proof_urls_json = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_after_sale'
        AND COLUMN_NAME = 'proof_urls_json'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_after_sale`
       ADD COLUMN `proof_urls_json` text COMMENT ''凭证图片JSON'' AFTER `reason_desc`'
  )
);
PREPARE stmt_add_after_sale_proof_urls_json FROM @ddl_add_after_sale_proof_urls_json;
EXECUTE stmt_add_after_sale_proof_urls_json;
DEALLOCATE PREPARE stmt_add_after_sale_proof_urls_json;

SET @ddl_add_after_sale_merchant_result = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_after_sale'
        AND COLUMN_NAME = 'merchant_result'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_after_sale`
       ADD COLUMN `merchant_result` varchar(512) DEFAULT NULL COMMENT ''商家处理说明'' AFTER `proof_urls_json`'
  )
);
PREPARE stmt_add_after_sale_merchant_result FROM @ddl_add_after_sale_merchant_result;
EXECUTE stmt_add_after_sale_merchant_result;
DEALLOCATE PREPARE stmt_add_after_sale_merchant_result;

SET @ddl_add_after_sale_return_company = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_after_sale'
        AND COLUMN_NAME = 'return_company'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_after_sale`
       ADD COLUMN `return_company` varchar(64) DEFAULT NULL COMMENT ''退货物流公司'' AFTER `merchant_result`'
  )
);
PREPARE stmt_add_after_sale_return_company FROM @ddl_add_after_sale_return_company;
EXECUTE stmt_add_after_sale_return_company;
DEALLOCATE PREPARE stmt_add_after_sale_return_company;

SET @ddl_add_after_sale_return_logistics_no = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_after_sale'
        AND COLUMN_NAME = 'return_logistics_no'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_after_sale`
       ADD COLUMN `return_logistics_no` varchar(64) DEFAULT NULL COMMENT ''退货物流单号'' AFTER `return_company`'
  )
);
PREPARE stmt_add_after_sale_return_logistics_no FROM @ddl_add_after_sale_return_logistics_no;
EXECUTE stmt_add_after_sale_return_logistics_no;
DEALLOCATE PREPARE stmt_add_after_sale_return_logistics_no;

SET @ddl_add_after_sale_returned_at = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_after_sale'
        AND COLUMN_NAME = 'returned_at'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_after_sale`
       ADD COLUMN `returned_at` datetime DEFAULT NULL COMMENT ''用户退货时间'' AFTER `return_logistics_no`'
  )
);
PREPARE stmt_add_after_sale_returned_at FROM @ddl_add_after_sale_returned_at;
EXECUTE stmt_add_after_sale_returned_at;
DEALLOCATE PREPARE stmt_add_after_sale_returned_at;

SET @ddl_add_after_sale_approved_at = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_after_sale'
        AND COLUMN_NAME = 'approved_at'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_after_sale`
       ADD COLUMN `approved_at` datetime DEFAULT NULL COMMENT ''审核通过时间'' AFTER `returned_at`'
  )
);
PREPARE stmt_add_after_sale_approved_at FROM @ddl_add_after_sale_approved_at;
EXECUTE stmt_add_after_sale_approved_at;
DEALLOCATE PREPARE stmt_add_after_sale_approved_at;

SET @ddl_add_after_sale_completed_at = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_after_sale'
        AND COLUMN_NAME = 'completed_at'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_after_sale`
       ADD COLUMN `completed_at` datetime DEFAULT NULL COMMENT ''售后完成时间'' AFTER `approved_at`'
  )
);
PREPARE stmt_add_after_sale_completed_at FROM @ddl_add_after_sale_completed_at;
EXECUTE stmt_add_after_sale_completed_at;
DEALLOCATE PREPARE stmt_add_after_sale_completed_at;

SET @ddl_add_after_sale_closed_at = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_after_sale'
        AND COLUMN_NAME = 'closed_at'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_after_sale`
       ADD COLUMN `closed_at` datetime DEFAULT NULL COMMENT ''售后关闭时间'' AFTER `completed_at`'
  )
);
PREPARE stmt_add_after_sale_closed_at FROM @ddl_add_after_sale_closed_at;
EXECUTE stmt_add_after_sale_closed_at;
DEALLOCATE PREPARE stmt_add_after_sale_closed_at;

SET @ddl_add_after_sale_order_item_status_index = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cdd_order_after_sale'
        AND INDEX_NAME = 'idx_order_item_status'
    ),
    'SELECT 1',
    'ALTER TABLE `cdd_order_after_sale`
       ADD KEY `idx_order_item_status` (`order_item_id`, `after_sale_status`)'
  )
);
PREPARE stmt_add_after_sale_order_item_status_index FROM @ddl_add_after_sale_order_item_status_index;
EXECUTE stmt_add_after_sale_order_item_status_index;
DEALLOCATE PREPARE stmt_add_after_sale_order_item_status_index;
