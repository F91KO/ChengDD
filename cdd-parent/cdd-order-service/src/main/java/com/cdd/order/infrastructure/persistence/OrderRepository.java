package com.cdd.order.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Optional<CartItem> findCartItem(long userId, long storeId, long skuId);

    void createCartItem(CartItem cartItem);

    void updateCartItem(CartItem cartItem);

    List<CartItem> listCartItems(long merchantId, long storeId, long userId);

    List<CartItem> listSelectedValidCartItems(long merchantId, long storeId, long userId);

    void createCheckoutSnapshot(CheckoutSnapshot snapshot);

    Optional<CheckoutSnapshot> findCheckoutSnapshotByToken(String snapshotToken);

    void createOrder(OrderRecord order, List<OrderItemRecord> items);

    void markCartItemsDeleted(List<Long> cartItemIds);

    void createStatusLog(OrderStatusLogRecord statusLog);

    Optional<OrderRecord> findOrderByOrderNo(String orderNo, long merchantId, long storeId, long userId);

    Optional<OrderRecord> findOrderById(long orderId);

    List<OrderRecord> listOrders(long merchantId, long storeId, Long userId, String orderStatus);

    List<OrderItemRecord> listOrderItems(long orderId);

    Optional<OrderItemRecord> findOrderItem(long orderId, long orderItemId);

    List<OrderStatusLogRecord> listOrderStatusLogs(long orderId);

    Optional<PayRecord> findPayRecordByPayNo(String payNo);

    Optional<PayRecord> findLatestPayRecordByOrderId(long orderId);

    void createPayRecord(PayRecord payRecord);

    boolean createPayCallbackRecordIfAbsent(PayCallbackRecord callbackRecord);

    Optional<PayCallbackRecord> findPayCallbackRecord(String payNo, String callbackEventId);

    void markPayCallbackProcessed(long callbackRecordId,
                                  String callbackStatus,
                                  String processedResult,
                                  String failureReason,
                                  Instant processedAt);

    boolean updateOrderPayStatusToPaying(long orderId);

    void updatePayRecordSuccess(long payRecordId, String thirdPartyTradeNo, String payResponseJson, Instant paidAt);

    boolean updateOrderToPaid(long orderId, BigDecimal paidAmount, Instant paidAt);

    boolean updateOrderToCancelled(long orderId, Instant cancelledAt);

    void closePayRecordsByOrderId(long orderId);

    boolean updateOrderDeliveryStatus(long orderId,
                                      String expectedOrderStatus,
                                      String expectedDeliveryStatus,
                                      String targetOrderStatus,
                                      String targetDeliveryStatus,
                                      Instant finishedAt);

    void createRefundRecord(RefundRecord refundRecord);

    Optional<RefundRecord> findRefundRecordByRefundNo(String refundNo);

    BigDecimal sumRefundAmountByOrderIdAndStatuses(long orderId, List<String> refundStatuses);

    BigDecimal sumRefundAmountByOrderItemIdAndStatuses(long orderItemId, List<String> refundStatuses);

    int sumRefundQuantityByOrderItemIdAndStatuses(long orderItemId, List<String> refundStatuses);

    boolean updateRefundRecordToSuccess(long refundRecordId, String thirdPartyRefundNo, Instant successAt);

    boolean updateRefundRecordToFailed(long refundRecordId, String failureReason);

    void bindRefundCompensationTask(long refundRecordId, String compensationTaskCode);

    boolean createRefundCallbackRecordIfAbsent(RefundCallbackRecord callbackRecord);

    Optional<RefundCallbackRecord> findRefundCallbackRecord(String refundNo, String callbackEventId);

    void markRefundCallbackProcessed(long callbackRecordId,
                                     String callbackStatus,
                                     String processedResult,
                                     String failureReason,
                                     Instant processedAt);

    boolean updateOrderPayStatusAfterRefund(long orderId, String targetPayStatus);

    void updateOrderItemsRefundStatus(long orderId, String refundStatus);

    void updateOrderItemRefundSnapshot(long orderItemId,
                                       String refundStatus,
                                       int refundedQuantity,
                                       BigDecimal refundedAmount);

    void createAfterSaleRecord(AfterSaleRecord afterSaleRecord);

    Optional<AfterSaleRecord> findAfterSaleByAfterSaleNo(String afterSaleNo);

    List<AfterSaleSummaryRecord> listAfterSales(long merchantId, long storeId, String afterSaleStatus);

    void updateAfterSaleStatus(long afterSaleId,
                               String afterSaleStatus,
                               String merchantResult,
                               Long handledBy,
                               Instant handledAt,
                               Instant approvedAt,
                               Instant completedAt,
                               Instant closedAt);

    void bindAfterSaleRefundRecord(long afterSaleId, Long refundRecordId, String refundNo);

    void updateAfterSaleReturnInfo(long afterSaleId,
                                   String returnCompany,
                                   String returnLogisticsNo,
                                   Instant returnedAt);

    boolean createCompensationTaskIfAbsent(CompensationTaskRecord compensationTaskRecord);

    Optional<CompensationTaskRecord> findCompensationTask(String bizType, String bizId, String compensationType);

    record CartItem(
            long id,
            long merchantId,
            long storeId,
            long userId,
            long productId,
            long skuId,
            int quantity,
            boolean selected,
            String invalidStatus,
            BigDecimal snapshotPrice) {
    }

    record CheckoutSnapshot(
            long id,
            long merchantId,
            long storeId,
            long userId,
            String snapshotToken,
            String cartItemIdsJson,
            String pricingSnapshotJson,
            Instant expiredAt) {
    }

    record OrderRecord(
            long id,
            String orderNo,
            long merchantId,
            long storeId,
            long userId,
            Long checkoutSnapshotId,
            String orderStatus,
            String payStatus,
            String deliveryStatus,
            String buyerRemark,
            BigDecimal totalAmount,
            BigDecimal discountAmount,
            BigDecimal payableAmount,
            BigDecimal paidAmount,
            BigDecimal deliveryFeeAmount,
            String receiverName,
            String receiverMobile,
            String receiverAddress,
            Instant createdAt,
            Instant paidAt,
            Instant cancelledAt,
            Instant finishedAt) {
    }

    record OrderItemRecord(
            long id,
            long orderId,
            long merchantId,
            long storeId,
            long productId,
            long skuId,
            String productName,
            String skuName,
            String skuSpecJson,
            BigDecimal salePrice,
            int quantity,
            BigDecimal lineAmount,
            String refundStatus,
            int refundedQuantity,
            BigDecimal refundedAmount) {
    }

    record OrderStatusLogRecord(
            long id,
            long orderId,
            String orderNo,
            String fromStatus,
            String toStatus,
            String operateType,
            Long operatorId,
            String operatorName,
            String remark,
            Instant createdAt) {
    }

    record PayRecord(
            long id,
            String payNo,
            long orderId,
            String orderNo,
            long merchantId,
            long storeId,
            String payChannel,
            String payMethod,
            String payStatus,
            BigDecimal payAmount) {
    }

    record PayCallbackRecord(
            long id,
            Long payRecordId,
            String payNo,
            long merchantId,
            Long storeId,
            String payChannel,
            String thirdPartyTradeNo,
            String callbackEventId,
            String callbackStatus,
            String processedResult,
            String failureReason,
            String callbackPayloadJson,
            Instant processedAt) {
    }

    record RefundRecord(
            long id,
            String refundNo,
            long orderId,
            String orderNo,
            Long payRecordId,
            Long afterSaleId,
            Long orderItemId,
            long merchantId,
            long storeId,
            String refundReason,
            String refundStatus,
            Integer refundQuantity,
            BigDecimal refundAmount,
            String thirdPartyRefundNo,
            Instant appliedAt,
            Instant successAt,
            String failureReason,
            String compensationTaskCode) {
    }

    record AfterSaleRecord(
            long id,
            String afterSaleNo,
            long orderId,
            String orderNo,
            Long orderItemId,
            long merchantId,
            long storeId,
            long userId,
            String afterSaleType,
            String afterSaleStatus,
            String reasonCode,
            String reasonDesc,
            String proofUrlsJson,
            Integer refundQuantity,
            BigDecimal refundAmount,
            String merchantResult,
            Long refundRecordId,
            String refundNo,
            String returnCompany,
            String returnLogisticsNo,
            Instant returnedAt,
            Instant approvedAt,
            Instant completedAt,
            Instant closedAt,
            Long handledBy,
            Instant handledAt) {
    }

    record AfterSaleSummaryRecord(
            String afterSaleNo,
            String orderNo,
            Long orderItemId,
            long merchantId,
            long storeId,
            long userId,
            String afterSaleType,
            String afterSaleStatus,
            String productName,
            String skuName,
            Integer refundQuantity,
            BigDecimal refundAmount,
            String reasonCode,
            String reasonDesc,
            String merchantResult,
            String refundNo,
            String returnCompany,
            String returnLogisticsNo,
            Long handledBy,
            Instant handledAt,
            Instant approvedAt,
            Instant returnedAt,
            Instant completedAt,
            Instant updatedAt) {
    }

    record RefundCallbackRecord(
            long id,
            Long refundRecordId,
            String refundNo,
            Long orderId,
            long merchantId,
            Long storeId,
            String thirdPartyRefundNo,
            String callbackEventId,
            String callbackStatus,
            String processedResult,
            String failureReason,
            String callbackPayloadJson,
            Instant processedAt) {
    }

    record CompensationTaskRecord(
            long id,
            String taskCode,
            String bizType,
            String bizId,
            String compensationType,
            String taskStatus,
            int retryCount,
            int maxRetryCount,
            Instant nextRetryAt,
            String lastErrorCode,
            String lastErrorMessage,
            String payloadJson,
            Instant resolvedAt,
            Long createdBy,
            Long updatedBy) {
    }
}
