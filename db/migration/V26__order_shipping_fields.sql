SET @ddl_add_order_logistics_company_code = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'cdd_order_info'
              AND COLUMN_NAME = 'logistics_company_code'
        ),
        'SELECT 1',
        'ALTER TABLE `cdd_order_info`
           ADD COLUMN `logistics_company_code` varchar(64) DEFAULT NULL COMMENT ''物流公司编码'' AFTER `receiver_address`'
    )
);
PREPARE stmt_add_order_logistics_company_code FROM @ddl_add_order_logistics_company_code;
EXECUTE stmt_add_order_logistics_company_code;
DEALLOCATE PREPARE stmt_add_order_logistics_company_code;

SET @ddl_add_order_logistics_company_name = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'cdd_order_info'
              AND COLUMN_NAME = 'logistics_company_name'
        ),
        'SELECT 1',
        'ALTER TABLE `cdd_order_info`
           ADD COLUMN `logistics_company_name` varchar(128) DEFAULT NULL COMMENT ''物流公司名称'' AFTER `logistics_company_code`'
    )
);
PREPARE stmt_add_order_logistics_company_name FROM @ddl_add_order_logistics_company_name;
EXECUTE stmt_add_order_logistics_company_name;
DEALLOCATE PREPARE stmt_add_order_logistics_company_name;

SET @ddl_add_order_tracking_no = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'cdd_order_info'
              AND COLUMN_NAME = 'tracking_no'
        ),
        'SELECT 1',
        'ALTER TABLE `cdd_order_info`
           ADD COLUMN `tracking_no` varchar(64) DEFAULT NULL COMMENT ''物流单号'' AFTER `logistics_company_name`'
    )
);
PREPARE stmt_add_order_tracking_no FROM @ddl_add_order_tracking_no;
EXECUTE stmt_add_order_tracking_no;
DEALLOCATE PREPARE stmt_add_order_tracking_no;

SET @ddl_add_order_shipped_at = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'cdd_order_info'
              AND COLUMN_NAME = 'shipped_at'
        ),
        'SELECT 1',
        'ALTER TABLE `cdd_order_info`
           ADD COLUMN `shipped_at` datetime DEFAULT NULL COMMENT ''发货时间'' AFTER `paid_at`'
    )
);
PREPARE stmt_add_order_shipped_at FROM @ddl_add_order_shipped_at;
EXECUTE stmt_add_order_shipped_at;
DEALLOCATE PREPARE stmt_add_order_shipped_at;
