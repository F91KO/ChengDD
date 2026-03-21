package com.cdd.order.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class JdbcOrderRepository implements OrderRepository {

    private static final RowMapper<CartItem> CART_ITEM_ROW_MAPPER = JdbcOrderRepository::mapCartItem;
    private static final RowMapper<CheckoutSnapshot> CHECKOUT_SNAPSHOT_ROW_MAPPER = JdbcOrderRepository::mapCheckoutSnapshot;
    private static final RowMapper<OrderRecord> ORDER_ROW_MAPPER = JdbcOrderRepository::mapOrder;
    private static final RowMapper<OrderItemRecord> ORDER_ITEM_ROW_MAPPER = JdbcOrderRepository::mapOrderItem;
    private static final RowMapper<OrderStatusLogRecord> ORDER_STATUS_LOG_ROW_MAPPER = JdbcOrderRepository::mapOrderStatusLog;
    private static final RowMapper<PayRecord> PAY_RECORD_ROW_MAPPER = JdbcOrderRepository::mapPayRecord;
    private static final RowMapper<PayCallbackRecord> PAY_CALLBACK_ROW_MAPPER = JdbcOrderRepository::mapPayCallbackRecord;
    private static final RowMapper<RefundRecord> REFUND_ROW_MAPPER = JdbcOrderRepository::mapRefundRecord;
    private static final RowMapper<RefundCallbackRecord> REFUND_CALLBACK_ROW_MAPPER = JdbcOrderRepository::mapRefundCallbackRecord;
    private static final RowMapper<CompensationTaskRecord> COMPENSATION_TASK_ROW_MAPPER = JdbcOrderRepository::mapCompensationTaskRecord;

    private final JdbcTemplate jdbcTemplate;

    public JdbcOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CartItem> findCartItem(long userId, long storeId, long skuId) {
        List<CartItem> rows = jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, user_id, product_id, sku_id, quantity, selected, invalid_status, snapshot_price
                FROM cdd_order_cart_item
                WHERE user_id = ?
                  AND store_id = ?
                  AND sku_id = ?
                  AND deleted = 0
                LIMIT 1
                """, CART_ITEM_ROW_MAPPER, userId, storeId, skuId);
        return rows.stream().findFirst();
    }

    @Override
    public void createCartItem(CartItem cartItem) {
        jdbcTemplate.update("""
                INSERT INTO cdd_order_cart_item (
                  id, merchant_id, store_id, user_id, product_id, sku_id, quantity, selected, invalid_status, snapshot_price,
                  created_by, updated_by, deleted, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                cartItem.id(),
                cartItem.merchantId(),
                cartItem.storeId(),
                cartItem.userId(),
                cartItem.productId(),
                cartItem.skuId(),
                cartItem.quantity(),
                cartItem.selected() ? 1 : 0,
                cartItem.invalidStatus(),
                cartItem.snapshotPrice(),
                cartItem.userId(),
                cartItem.userId(),
                0,
                0L);
    }

    @Override
    public void updateCartItem(CartItem cartItem) {
        jdbcTemplate.update("""
                UPDATE cdd_order_cart_item
                SET quantity = ?,
                    selected = ?,
                    invalid_status = ?,
                    snapshot_price = ?,
                    updated_by = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                """,
                cartItem.quantity(),
                cartItem.selected() ? 1 : 0,
                cartItem.invalidStatus(),
                cartItem.snapshotPrice(),
                cartItem.userId(),
                cartItem.id());
    }

    @Override
    public List<CartItem> listCartItems(long merchantId, long storeId, long userId) {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, user_id, product_id, sku_id, quantity, selected, invalid_status, snapshot_price
                FROM cdd_order_cart_item
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND user_id = ?
                  AND deleted = 0
                ORDER BY id ASC
                """, CART_ITEM_ROW_MAPPER, merchantId, storeId, userId);
    }

    @Override
    public List<CartItem> listSelectedValidCartItems(long merchantId, long storeId, long userId) {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, user_id, product_id, sku_id, quantity, selected, invalid_status, snapshot_price
                FROM cdd_order_cart_item
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND user_id = ?
                  AND selected = 1
                  AND invalid_status = 'valid'
                  AND deleted = 0
                ORDER BY id ASC
                """, CART_ITEM_ROW_MAPPER, merchantId, storeId, userId);
    }

    @Override
    public void createCheckoutSnapshot(CheckoutSnapshot snapshot) {
        jdbcTemplate.update("""
                INSERT INTO cdd_order_checkout_snapshot (
                  id, merchant_id, store_id, user_id, snapshot_token, cart_item_ids_json, pricing_snapshot_json, expired_at,
                  created_by, updated_by, deleted, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                snapshot.id(),
                snapshot.merchantId(),
                snapshot.storeId(),
                snapshot.userId(),
                snapshot.snapshotToken(),
                snapshot.cartItemIdsJson(),
                snapshot.pricingSnapshotJson(),
                toTimestamp(snapshot.expiredAt()),
                snapshot.userId(),
                snapshot.userId(),
                0,
                0L);
    }

    @Override
    public Optional<CheckoutSnapshot> findCheckoutSnapshotByToken(String snapshotToken) {
        List<CheckoutSnapshot> rows = jdbcTemplate.query("""
                SELECT id, merchant_id, store_id, user_id, snapshot_token, cart_item_ids_json, pricing_snapshot_json, expired_at
                FROM cdd_order_checkout_snapshot
                WHERE snapshot_token = ?
                  AND deleted = 0
                LIMIT 1
                """, CHECKOUT_SNAPSHOT_ROW_MAPPER, snapshotToken);
        return rows.stream().findFirst();
    }

    @Override
    public void createOrder(OrderRecord order, List<OrderItemRecord> items) {
        jdbcTemplate.update("""
                INSERT INTO cdd_order_info (
                  id, order_no, merchant_id, store_id, user_id, checkout_snapshot_id, order_status, pay_status, delivery_status,
                  buyer_remark, total_amount, discount_amount, payable_amount, paid_amount, delivery_fee_amount,
                  receiver_name, receiver_mobile, receiver_address, paid_at, cancelled_at, finished_at, created_by, updated_by,
                  deleted, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                order.id(),
                order.orderNo(),
                order.merchantId(),
                order.storeId(),
                order.userId(),
                order.checkoutSnapshotId(),
                order.orderStatus(),
                order.payStatus(),
                order.deliveryStatus(),
                order.buyerRemark(),
                order.totalAmount(),
                order.discountAmount(),
                order.payableAmount(),
                order.paidAmount(),
                order.deliveryFeeAmount(),
                order.receiverName(),
                order.receiverMobile(),
                order.receiverAddress(),
                toTimestamp(order.paidAt()),
                toTimestamp(order.cancelledAt()),
                toTimestamp(order.finishedAt()),
                order.userId(),
                order.userId(),
                0,
                0L);

        if (items.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO cdd_order_item (
                  id, order_id, merchant_id, store_id, product_id, sku_id, product_name, sku_name, sku_spec_json,
                  sale_price, quantity, line_amount, refund_status, created_by, updated_by, deleted, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, items, items.size(), (ps, item) -> {
            ps.setLong(1, item.id());
            ps.setLong(2, item.orderId());
            ps.setLong(3, item.merchantId());
            ps.setLong(4, item.storeId());
            ps.setLong(5, item.productId());
            ps.setLong(6, item.skuId());
            ps.setString(7, item.productName());
            ps.setString(8, item.skuName());
            ps.setString(9, item.skuSpecJson());
            ps.setBigDecimal(10, item.salePrice());
            ps.setInt(11, item.quantity());
            ps.setBigDecimal(12, item.lineAmount());
            ps.setString(13, item.refundStatus());
            ps.setLong(14, item.merchantId());
            ps.setLong(15, item.merchantId());
            ps.setInt(16, 0);
            ps.setLong(17, 0L);
        });
    }

    @Override
    public void markCartItemsDeleted(List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(cartItemIds.size(), "?"));
        List<Object> args = new ArrayList<>(cartItemIds);
        jdbcTemplate.update("""
                UPDATE cdd_order_cart_item
                SET deleted = 1, updated_at = CURRENT_TIMESTAMP
                WHERE id IN (""" + placeholders + ") AND deleted = 0", args.toArray());
    }

    @Override
    public void createStatusLog(OrderStatusLogRecord statusLog) {
        jdbcTemplate.update("""
                INSERT INTO cdd_order_status_log (
                  id, order_id, order_no, from_status, to_status, operate_type, operator_id, operator_name, remark,
                  created_by, updated_by, deleted, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                statusLog.id(),
                statusLog.orderId(),
                statusLog.orderNo(),
                statusLog.fromStatus(),
                statusLog.toStatus(),
                statusLog.operateType(),
                statusLog.operatorId(),
                statusLog.operatorName(),
                statusLog.remark(),
                statusLog.operatorId(),
                statusLog.operatorId(),
                0,
                0L);
    }

    @Override
    public Optional<OrderRecord> findOrderByOrderNo(String orderNo, long merchantId, long storeId, long userId) {
        List<OrderRecord> rows = jdbcTemplate.query("""
                SELECT id, order_no, merchant_id, store_id, user_id, checkout_snapshot_id, order_status, pay_status, delivery_status,
                       buyer_remark, total_amount, discount_amount, payable_amount, paid_amount, delivery_fee_amount,
                       receiver_name, receiver_mobile, receiver_address, created_at, paid_at, cancelled_at, finished_at
                FROM cdd_order_info
                WHERE order_no = ?
                  AND merchant_id = ?
                  AND store_id = ?
                  AND user_id = ?
                  AND deleted = 0
                LIMIT 1
                """, ORDER_ROW_MAPPER, orderNo, merchantId, storeId, userId);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<OrderRecord> findOrderById(long orderId) {
        List<OrderRecord> rows = jdbcTemplate.query("""
                SELECT id, order_no, merchant_id, store_id, user_id, checkout_snapshot_id, order_status, pay_status, delivery_status,
                       buyer_remark, total_amount, discount_amount, payable_amount, paid_amount, delivery_fee_amount,
                       receiver_name, receiver_mobile, receiver_address, created_at, paid_at, cancelled_at, finished_at
                FROM cdd_order_info
                WHERE id = ?
                  AND deleted = 0
                LIMIT 1
                """, ORDER_ROW_MAPPER, orderId);
        return rows.stream().findFirst();
    }

    @Override
    public List<OrderRecord> listOrders(long merchantId, long storeId, Long userId, String orderStatus) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, order_no, merchant_id, store_id, user_id, checkout_snapshot_id, order_status, pay_status, delivery_status,
                       buyer_remark, total_amount, discount_amount, payable_amount, paid_amount, delivery_fee_amount,
                       receiver_name, receiver_mobile, receiver_address, created_at, paid_at, cancelled_at, finished_at
                FROM cdd_order_info
                WHERE merchant_id = ?
                  AND store_id = ?
                  AND deleted = 0
                """);
        List<Object> args = new ArrayList<>();
        args.add(merchantId);
        args.add(storeId);
        if (userId != null) {
            sql.append(" AND user_id = ?");
            args.add(userId);
        }
        if (StringUtils.hasText(orderStatus)) {
            sql.append(" AND order_status = ?");
            args.add(orderStatus.trim().toLowerCase());
        }
        sql.append(" ORDER BY created_at DESC, id DESC");
        return jdbcTemplate.query(sql.toString(), ORDER_ROW_MAPPER, args.toArray());
    }

    @Override
    public List<OrderItemRecord> listOrderItems(long orderId) {
        return jdbcTemplate.query("""
                SELECT id, order_id, merchant_id, store_id, product_id, sku_id, product_name, sku_name, sku_spec_json,
                       sale_price, quantity, line_amount, refund_status
                FROM cdd_order_item
                WHERE order_id = ?
                  AND deleted = 0
                ORDER BY id ASC
                """, ORDER_ITEM_ROW_MAPPER, orderId);
    }

    @Override
    public List<OrderStatusLogRecord> listOrderStatusLogs(long orderId) {
        return jdbcTemplate.query("""
                SELECT id, order_id, order_no, from_status, to_status, operate_type, operator_id, operator_name, remark, created_at
                FROM cdd_order_status_log
                WHERE order_id = ?
                  AND deleted = 0
                ORDER BY created_at ASC, id ASC
                """, ORDER_STATUS_LOG_ROW_MAPPER, orderId);
    }

    @Override
    public Optional<PayRecord> findPayRecordByPayNo(String payNo) {
        List<PayRecord> rows = jdbcTemplate.query("""
                SELECT id, pay_no, order_id, order_no, merchant_id, store_id, pay_channel, pay_method, pay_status, pay_amount
                FROM cdd_order_pay_record
                WHERE pay_no = ?
                  AND deleted = 0
                LIMIT 1
                """, PAY_RECORD_ROW_MAPPER, payNo);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<PayRecord> findLatestPayRecordByOrderId(long orderId) {
        List<PayRecord> rows = jdbcTemplate.query("""
                SELECT id, pay_no, order_id, order_no, merchant_id, store_id, pay_channel, pay_method, pay_status, pay_amount
                FROM cdd_order_pay_record
                WHERE order_id = ?
                  AND deleted = 0
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """, PAY_RECORD_ROW_MAPPER, orderId);
        return rows.stream().findFirst();
    }

    @Override
    public void createPayRecord(PayRecord payRecord) {
        jdbcTemplate.update("""
                INSERT INTO cdd_order_pay_record (
                  id, pay_no, order_id, order_no, merchant_id, store_id, pay_channel, pay_method, pay_status,
                  pay_amount, created_by, updated_by, deleted, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                payRecord.id(),
                payRecord.payNo(),
                payRecord.orderId(),
                payRecord.orderNo(),
                payRecord.merchantId(),
                payRecord.storeId(),
                payRecord.payChannel(),
                payRecord.payMethod(),
                payRecord.payStatus(),
                payRecord.payAmount(),
                payRecord.merchantId(),
                payRecord.merchantId(),
                0,
                0L);
    }

    @Override
    public boolean createPayCallbackRecordIfAbsent(PayCallbackRecord callbackRecord) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO cdd_order_pay_callback_record (
                      id, pay_record_id, pay_no, merchant_id, store_id, pay_channel, third_party_trade_no, callback_event_id,
                      callback_status, processed_result, failure_reason, callback_payload_json, processed_at,
                      created_by, updated_by, deleted, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    callbackRecord.id(),
                    callbackRecord.payRecordId(),
                    callbackRecord.payNo(),
                    callbackRecord.merchantId(),
                    callbackRecord.storeId(),
                    callbackRecord.payChannel(),
                    callbackRecord.thirdPartyTradeNo(),
                    callbackRecord.callbackEventId(),
                    callbackRecord.callbackStatus(),
                    callbackRecord.processedResult(),
                    callbackRecord.failureReason(),
                    callbackRecord.callbackPayloadJson(),
                    toTimestamp(callbackRecord.processedAt()),
                    callbackRecord.merchantId(),
                    callbackRecord.merchantId(),
                    0,
                    0L);
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    @Override
    public Optional<PayCallbackRecord> findPayCallbackRecord(String payNo, String callbackEventId) {
        List<PayCallbackRecord> rows = jdbcTemplate.query("""
                SELECT id, pay_record_id, pay_no, merchant_id, store_id, pay_channel, third_party_trade_no, callback_event_id,
                       callback_status, processed_result, failure_reason, callback_payload_json, processed_at
                FROM cdd_order_pay_callback_record
                WHERE pay_no = ?
                  AND callback_event_id = ?
                  AND deleted = 0
                LIMIT 1
                """, PAY_CALLBACK_ROW_MAPPER, payNo, callbackEventId);
        return rows.stream().findFirst();
    }

    @Override
    public void markPayCallbackProcessed(long callbackRecordId,
                                         String callbackStatus,
                                         String processedResult,
                                         String failureReason,
                                         Instant processedAt) {
        jdbcTemplate.update("""
                UPDATE cdd_order_pay_callback_record
                SET callback_status = ?,
                    processed_result = ?,
                    failure_reason = ?,
                    processed_at = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                """,
                callbackStatus,
                processedResult,
                failureReason,
                toTimestamp(processedAt),
                callbackRecordId);
    }

    @Override
    public boolean updateOrderPayStatusToPaying(long orderId) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_order_info
                SET pay_status = 'paying',
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                  AND order_status = 'pending_pay'
                  AND pay_status = 'unpaid'
                """, orderId);
        return updated > 0;
    }

    @Override
    public void updatePayRecordSuccess(long payRecordId, String thirdPartyTradeNo, String payResponseJson, Instant paidAt) {
        jdbcTemplate.update("""
                UPDATE cdd_order_pay_record
                SET pay_status = 'success',
                    third_party_trade_no = ?,
                    pay_response_json = ?,
                    paid_at = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                  AND pay_status IN ('created', 'paying', 'success')
                """,
                thirdPartyTradeNo,
                payResponseJson,
                toTimestamp(paidAt),
                payRecordId);
    }

    @Override
    public boolean updateOrderToPaid(long orderId, BigDecimal paidAmount, Instant paidAt) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_order_info
                SET order_status = 'paid',
                    pay_status = 'paid',
                    delivery_status = 'pending',
                    paid_amount = ?,
                    paid_at = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                  AND order_status = 'pending_pay'
                  AND pay_status IN ('unpaid', 'paying')
                """,
                paidAmount,
                toTimestamp(paidAt),
                orderId);
        return updated > 0;
    }

    @Override
    public boolean updateOrderToCancelled(long orderId, Instant cancelledAt) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_order_info
                SET order_status = 'cancelled',
                    cancelled_at = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                  AND order_status = 'pending_pay'
                  AND pay_status IN ('unpaid', 'paying')
                """,
                toTimestamp(cancelledAt),
                orderId);
        return updated > 0;
    }

    @Override
    public void closePayRecordsByOrderId(long orderId) {
        jdbcTemplate.update("""
                UPDATE cdd_order_pay_record
                SET pay_status = 'closed',
                    updated_at = CURRENT_TIMESTAMP
                WHERE order_id = ?
                  AND deleted = 0
                  AND pay_status IN ('created', 'paying')
                """, orderId);
    }

    @Override
    public void createRefundRecord(RefundRecord refundRecord) {
        jdbcTemplate.update("""
                INSERT INTO cdd_order_refund_record (
                  id, refund_no, order_id, order_no, pay_record_id, merchant_id, store_id, refund_reason, refund_status,
                  refund_amount, third_party_refund_no, applied_at, success_at, failure_reason, compensation_task_code,
                  created_by, updated_by, deleted, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                refundRecord.id(),
                refundRecord.refundNo(),
                refundRecord.orderId(),
                refundRecord.orderNo(),
                refundRecord.payRecordId(),
                refundRecord.merchantId(),
                refundRecord.storeId(),
                refundRecord.refundReason(),
                refundRecord.refundStatus(),
                refundRecord.refundAmount(),
                refundRecord.thirdPartyRefundNo(),
                toTimestamp(refundRecord.appliedAt()),
                toTimestamp(refundRecord.successAt()),
                refundRecord.failureReason(),
                refundRecord.compensationTaskCode(),
                refundRecord.merchantId(),
                refundRecord.merchantId(),
                0,
                0L);
    }

    @Override
    public Optional<RefundRecord> findRefundRecordByRefundNo(String refundNo) {
        List<RefundRecord> rows = jdbcTemplate.query("""
                SELECT id, refund_no, order_id, order_no, pay_record_id, merchant_id, store_id, refund_reason, refund_status,
                       refund_amount, third_party_refund_no, applied_at, success_at, failure_reason, compensation_task_code
                FROM cdd_order_refund_record
                WHERE refund_no = ?
                  AND deleted = 0
                LIMIT 1
                """, REFUND_ROW_MAPPER, refundNo);
        return rows.stream().findFirst();
    }

    @Override
    public BigDecimal sumRefundAmountByOrderIdAndStatuses(long orderId, List<String> refundStatuses) {
        if (refundStatuses == null || refundStatuses.isEmpty()) {
            return BigDecimal.ZERO;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(refundStatuses.size(), "?"));
        List<Object> args = new ArrayList<>();
        args.add(orderId);
        args.addAll(refundStatuses);
        BigDecimal total = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(refund_amount), 0.00)
                FROM cdd_order_refund_record
                WHERE order_id = ?
                  AND deleted = 0
                  AND refund_status IN (""" + placeholders + ")",
                BigDecimal.class,
                args.toArray());
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public boolean updateRefundRecordToSuccess(long refundRecordId, String thirdPartyRefundNo, Instant successAt) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_order_refund_record
                SET refund_status = 'success',
                    third_party_refund_no = COALESCE(?, third_party_refund_no),
                    success_at = ?,
                    failure_reason = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                  AND refund_status IN ('created', 'processing', 'failed', 'success')
                """,
                thirdPartyRefundNo,
                toTimestamp(successAt),
                refundRecordId);
        return updated > 0;
    }

    @Override
    public boolean updateRefundRecordToFailed(long refundRecordId, String failureReason) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_order_refund_record
                SET refund_status = 'failed',
                    failure_reason = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                  AND refund_status IN ('created', 'processing', 'failed')
                """,
                failureReason,
                refundRecordId);
        return updated > 0;
    }

    @Override
    public void bindRefundCompensationTask(long refundRecordId, String compensationTaskCode) {
        jdbcTemplate.update("""
                UPDATE cdd_order_refund_record
                SET compensation_task_code = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                """,
                compensationTaskCode,
                refundRecordId);
    }

    @Override
    public boolean createRefundCallbackRecordIfAbsent(RefundCallbackRecord callbackRecord) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO cdd_order_refund_callback_record (
                      id, refund_record_id, refund_no, order_id, merchant_id, store_id, third_party_refund_no, callback_event_id,
                      callback_status, processed_result, failure_reason, callback_payload_json, processed_at,
                      created_by, updated_by, deleted, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    callbackRecord.id(),
                    callbackRecord.refundRecordId(),
                    callbackRecord.refundNo(),
                    callbackRecord.orderId(),
                    callbackRecord.merchantId(),
                    callbackRecord.storeId(),
                    callbackRecord.thirdPartyRefundNo(),
                    callbackRecord.callbackEventId(),
                    callbackRecord.callbackStatus(),
                    callbackRecord.processedResult(),
                    callbackRecord.failureReason(),
                    callbackRecord.callbackPayloadJson(),
                    toTimestamp(callbackRecord.processedAt()),
                    callbackRecord.merchantId(),
                    callbackRecord.merchantId(),
                    0,
                    0L);
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    @Override
    public Optional<RefundCallbackRecord> findRefundCallbackRecord(String refundNo, String callbackEventId) {
        List<RefundCallbackRecord> rows = jdbcTemplate.query("""
                SELECT id, refund_record_id, refund_no, order_id, merchant_id, store_id, third_party_refund_no, callback_event_id,
                       callback_status, processed_result, failure_reason, callback_payload_json, processed_at
                FROM cdd_order_refund_callback_record
                WHERE refund_no = ?
                  AND callback_event_id = ?
                  AND deleted = 0
                LIMIT 1
                """, REFUND_CALLBACK_ROW_MAPPER, refundNo, callbackEventId);
        return rows.stream().findFirst();
    }

    @Override
    public void markRefundCallbackProcessed(long callbackRecordId,
                                            String callbackStatus,
                                            String processedResult,
                                            String failureReason,
                                            Instant processedAt) {
        jdbcTemplate.update("""
                UPDATE cdd_order_refund_callback_record
                SET callback_status = ?,
                    processed_result = ?,
                    failure_reason = ?,
                    processed_at = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                """,
                callbackStatus,
                processedResult,
                failureReason,
                toTimestamp(processedAt),
                callbackRecordId);
    }

    @Override
    public boolean updateOrderPayStatusAfterRefund(long orderId, String targetPayStatus) {
        int updated = jdbcTemplate.update("""
                UPDATE cdd_order_info
                SET pay_status = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND deleted = 0
                  AND pay_status IN ('paid', 'refund_partial', 'refund_full')
                """,
                targetPayStatus,
                orderId);
        return updated > 0;
    }

    @Override
    public void updateOrderItemsRefundStatus(long orderId, String refundStatus) {
        jdbcTemplate.update("""
                UPDATE cdd_order_item
                SET refund_status = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE order_id = ?
                  AND deleted = 0
                """,
                refundStatus,
                orderId);
    }

    @Override
    public boolean createCompensationTaskIfAbsent(CompensationTaskRecord compensationTaskRecord) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO cdd_compensation_task (
                      id, task_code, biz_type, biz_id, compensation_type, task_status, retry_count, max_retry_count,
                      next_retry_at, last_error_code, last_error_message, payload_json, resolved_at,
                      created_by, updated_by, deleted, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    compensationTaskRecord.id(),
                    compensationTaskRecord.taskCode(),
                    compensationTaskRecord.bizType(),
                    compensationTaskRecord.bizId(),
                    compensationTaskRecord.compensationType(),
                    compensationTaskRecord.taskStatus(),
                    compensationTaskRecord.retryCount(),
                    compensationTaskRecord.maxRetryCount(),
                    toTimestamp(compensationTaskRecord.nextRetryAt()),
                    compensationTaskRecord.lastErrorCode(),
                    compensationTaskRecord.lastErrorMessage(),
                    compensationTaskRecord.payloadJson(),
                    toTimestamp(compensationTaskRecord.resolvedAt()),
                    compensationTaskRecord.createdBy(),
                    compensationTaskRecord.updatedBy(),
                    0,
                    0L);
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    @Override
    public Optional<CompensationTaskRecord> findCompensationTask(String bizType, String bizId, String compensationType) {
        List<CompensationTaskRecord> rows = jdbcTemplate.query("""
                SELECT id, task_code, biz_type, biz_id, compensation_type, task_status, retry_count, max_retry_count,
                       next_retry_at, last_error_code, last_error_message, payload_json, resolved_at, created_by, updated_by
                FROM cdd_compensation_task
                WHERE biz_type = ?
                  AND biz_id = ?
                  AND compensation_type = ?
                  AND deleted = 0
                LIMIT 1
                """, COMPENSATION_TASK_ROW_MAPPER, bizType, bizId, compensationType);
        return rows.stream().findFirst();
    }

    @Override
    public boolean updateOrderDeliveryStatus(long orderId,
                                             String expectedOrderStatus,
                                             String expectedDeliveryStatus,
                                             String targetOrderStatus,
                                             String targetDeliveryStatus,
                                             Instant finishedAt) {
        StringBuilder sql = new StringBuilder("""
                UPDATE cdd_order_info
                SET order_status = ?,
                    delivery_status = ?,
                    updated_at = CURRENT_TIMESTAMP
                """);
        List<Object> args = new ArrayList<>();
        args.add(targetOrderStatus);
        args.add(targetDeliveryStatus);
        if (finishedAt != null) {
            sql.append(", finished_at = ?");
            args.add(toTimestamp(finishedAt));
        }
        sql.append("""
                WHERE id = ?
                  AND deleted = 0
                  AND order_status = ?
                """);
        args.add(orderId);
        args.add(expectedOrderStatus);
        if (expectedDeliveryStatus == null) {
            sql.append(" AND delivery_status IS NULL");
        } else {
            sql.append(" AND delivery_status = ?");
            args.add(expectedDeliveryStatus);
        }
        int updated = jdbcTemplate.update(sql.toString(), args.toArray());
        return updated > 0;
    }

    private static CartItem mapCartItem(ResultSet rs, int rowNum) throws SQLException {
        return new CartItem(
                rs.getLong("id"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getLong("user_id"),
                rs.getLong("product_id"),
                rs.getLong("sku_id"),
                rs.getInt("quantity"),
                rs.getBoolean("selected"),
                rs.getString("invalid_status"),
                rs.getBigDecimal("snapshot_price"));
    }

    private static CheckoutSnapshot mapCheckoutSnapshot(ResultSet rs, int rowNum) throws SQLException {
        return new CheckoutSnapshot(
                rs.getLong("id"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getLong("user_id"),
                rs.getString("snapshot_token"),
                rs.getString("cart_item_ids_json"),
                rs.getString("pricing_snapshot_json"),
                toInstant(rs.getTimestamp("expired_at")));
    }

    private static OrderRecord mapOrder(ResultSet rs, int rowNum) throws SQLException {
        long checkoutSnapshotId = rs.getLong("checkout_snapshot_id");
        return new OrderRecord(
                rs.getLong("id"),
                rs.getString("order_no"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getLong("user_id"),
                rs.wasNull() ? null : checkoutSnapshotId,
                rs.getString("order_status"),
                rs.getString("pay_status"),
                rs.getString("delivery_status"),
                rs.getString("buyer_remark"),
                rs.getBigDecimal("total_amount"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("payable_amount"),
                rs.getBigDecimal("paid_amount"),
                rs.getBigDecimal("delivery_fee_amount"),
                rs.getString("receiver_name"),
                rs.getString("receiver_mobile"),
                rs.getString("receiver_address"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("paid_at")),
                toInstant(rs.getTimestamp("cancelled_at")),
                toInstant(rs.getTimestamp("finished_at")));
    }

    private static OrderItemRecord mapOrderItem(ResultSet rs, int rowNum) throws SQLException {
        return new OrderItemRecord(
                rs.getLong("id"),
                rs.getLong("order_id"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getLong("product_id"),
                rs.getLong("sku_id"),
                rs.getString("product_name"),
                rs.getString("sku_name"),
                rs.getString("sku_spec_json"),
                rs.getBigDecimal("sale_price"),
                rs.getInt("quantity"),
                rs.getBigDecimal("line_amount"),
                rs.getString("refund_status"));
    }

    private static OrderStatusLogRecord mapOrderStatusLog(ResultSet rs, int rowNum) throws SQLException {
        Long operatorId = rs.getObject("operator_id", Long.class);
        return new OrderStatusLogRecord(
                rs.getLong("id"),
                rs.getLong("order_id"),
                rs.getString("order_no"),
                rs.getString("from_status"),
                rs.getString("to_status"),
                rs.getString("operate_type"),
                operatorId,
                rs.getString("operator_name"),
                rs.getString("remark"),
                toInstant(rs.getTimestamp("created_at")));
    }

    private static PayRecord mapPayRecord(ResultSet rs, int rowNum) throws SQLException {
        return new PayRecord(
                rs.getLong("id"),
                rs.getString("pay_no"),
                rs.getLong("order_id"),
                rs.getString("order_no"),
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getString("pay_channel"),
                rs.getString("pay_method"),
                rs.getString("pay_status"),
                rs.getBigDecimal("pay_amount"));
    }

    private static PayCallbackRecord mapPayCallbackRecord(ResultSet rs, int rowNum) throws SQLException {
        Long payRecordId = rs.getObject("pay_record_id", Long.class);
        Long storeId = rs.getObject("store_id", Long.class);
        return new PayCallbackRecord(
                rs.getLong("id"),
                payRecordId,
                rs.getString("pay_no"),
                rs.getLong("merchant_id"),
                storeId,
                rs.getString("pay_channel"),
                rs.getString("third_party_trade_no"),
                rs.getString("callback_event_id"),
                rs.getString("callback_status"),
                rs.getString("processed_result"),
                rs.getString("failure_reason"),
                rs.getString("callback_payload_json"),
                toInstant(rs.getTimestamp("processed_at")));
    }

    private static RefundRecord mapRefundRecord(ResultSet rs, int rowNum) throws SQLException {
        Long payRecordId = rs.getObject("pay_record_id", Long.class);
        return new RefundRecord(
                rs.getLong("id"),
                rs.getString("refund_no"),
                rs.getLong("order_id"),
                rs.getString("order_no"),
                payRecordId,
                rs.getLong("merchant_id"),
                rs.getLong("store_id"),
                rs.getString("refund_reason"),
                rs.getString("refund_status"),
                rs.getBigDecimal("refund_amount"),
                rs.getString("third_party_refund_no"),
                toInstant(rs.getTimestamp("applied_at")),
                toInstant(rs.getTimestamp("success_at")),
                rs.getString("failure_reason"),
                rs.getString("compensation_task_code"));
    }

    private static RefundCallbackRecord mapRefundCallbackRecord(ResultSet rs, int rowNum) throws SQLException {
        Long refundRecordId = rs.getObject("refund_record_id", Long.class);
        Long orderId = rs.getObject("order_id", Long.class);
        Long storeId = rs.getObject("store_id", Long.class);
        return new RefundCallbackRecord(
                rs.getLong("id"),
                refundRecordId,
                rs.getString("refund_no"),
                orderId,
                rs.getLong("merchant_id"),
                storeId,
                rs.getString("third_party_refund_no"),
                rs.getString("callback_event_id"),
                rs.getString("callback_status"),
                rs.getString("processed_result"),
                rs.getString("failure_reason"),
                rs.getString("callback_payload_json"),
                toInstant(rs.getTimestamp("processed_at")));
    }

    private static CompensationTaskRecord mapCompensationTaskRecord(ResultSet rs, int rowNum) throws SQLException {
        Integer retryCount = rs.getObject("retry_count", Integer.class);
        Integer maxRetryCount = rs.getObject("max_retry_count", Integer.class);
        Long createdBy = rs.getObject("created_by", Long.class);
        Long updatedBy = rs.getObject("updated_by", Long.class);
        return new CompensationTaskRecord(
                rs.getLong("id"),
                rs.getString("task_code"),
                rs.getString("biz_type"),
                rs.getString("biz_id"),
                rs.getString("compensation_type"),
                rs.getString("task_status"),
                retryCount == null ? 0 : retryCount,
                maxRetryCount == null ? 0 : maxRetryCount,
                toInstant(rs.getTimestamp("next_retry_at")),
                rs.getString("last_error_code"),
                rs.getString("last_error_message"),
                rs.getString("payload_json"),
                toInstant(rs.getTimestamp("resolved_at")),
                createdBy,
                updatedBy);
    }

    private static Timestamp toTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant toInstant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
