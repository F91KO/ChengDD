SET NAMES utf8mb4;

UPDATE `cdd_order_item`
SET
  `refund_status` = 'partial_refunding',
  `refunded_quantity` = 0,
  `refunded_amount` = 0.00,
  `updated_by` = 1001,
  `updated_at` = '2026-03-28 10:00:00'
WHERE `id` = 9181102
  AND `deleted` = 0;
