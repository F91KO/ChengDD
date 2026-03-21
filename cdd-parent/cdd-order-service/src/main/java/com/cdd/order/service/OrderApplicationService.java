package com.cdd.order.service;

import com.cdd.api.order.model.CartItemResponse;
import com.cdd.api.order.model.CartItemUpsertRequest;
import com.cdd.api.order.model.CheckoutRequest;
import com.cdd.api.order.model.CheckoutResponse;
import com.cdd.api.order.model.CreateOrderRequest;
import com.cdd.api.order.model.CreateOrderResponse;
import com.cdd.api.order.model.OrderCancelRequest;
import com.cdd.api.order.model.OrderAfterSaleCreateRequest;
import com.cdd.api.order.model.OrderAfterSaleLifecycleResponse;
import com.cdd.api.order.model.OrderAfterSaleReturnRequest;
import com.cdd.api.order.model.OrderAfterSaleReviewRequest;
import com.cdd.api.order.model.OrderDeliveryUpdateRequest;
import com.cdd.api.order.model.OrderDetailResponse;
import com.cdd.api.order.model.OrderItemResponse;
import com.cdd.api.order.model.OrderLifecycleResponse;
import com.cdd.api.order.model.OrderPayCallbackRequest;
import com.cdd.api.order.model.OrderPayCallbackResponse;
import com.cdd.api.order.model.OrderPaySuccessRequest;
import com.cdd.api.order.model.OrderPayingRequest;
import com.cdd.api.order.model.OrderPayingResponse;
import com.cdd.api.order.model.OrderRefundCallbackRequest;
import com.cdd.api.order.model.OrderRefundCreateRequest;
import com.cdd.api.order.model.OrderRefundLifecycleResponse;
import com.cdd.api.order.model.OrderStatusLogResponse;
import com.cdd.api.order.model.OrderSummaryResponse;
import com.cdd.common.core.error.BusinessException;
import com.cdd.order.error.OrderErrorCode;
import com.cdd.order.infrastructure.persistence.OrderRepository.CompensationTaskRecord;
import com.cdd.order.infrastructure.persistence.OrderRepository;
import com.cdd.order.infrastructure.persistence.OrderRepository.AfterSaleRecord;
import com.cdd.order.infrastructure.persistence.OrderRepository.CartItem;
import com.cdd.order.infrastructure.persistence.OrderRepository.CheckoutSnapshot;
import com.cdd.order.infrastructure.persistence.OrderRepository.OrderItemRecord;
import com.cdd.order.infrastructure.persistence.OrderRepository.OrderRecord;
import com.cdd.order.infrastructure.persistence.OrderRepository.PayCallbackRecord;
import com.cdd.order.infrastructure.persistence.OrderRepository.OrderStatusLogRecord;
import com.cdd.order.infrastructure.persistence.OrderRepository.PayRecord;
import com.cdd.order.infrastructure.persistence.OrderRepository.RefundCallbackRecord;
import com.cdd.order.infrastructure.persistence.OrderRepository.RefundRecord;
import com.cdd.order.support.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OrderApplicationService {

    private static final String CART_STATUS_VALID = "valid";
    private static final String ORDER_STATUS_PENDING_PAY = "pending_pay";
    private static final String ORDER_STATUS_PAID = "paid";
    private static final String ORDER_STATUS_PREPARING = "preparing";
    private static final String ORDER_STATUS_SHIPPED = "shipped";
    private static final String ORDER_STATUS_FINISHED = "finished";
    private static final String ORDER_STATUS_CANCELLED = "cancelled";
    private static final String PAY_STATUS_UNPAID = "unpaid";
    private static final String PAY_STATUS_PAYING = "paying";
    private static final String PAY_STATUS_PAID = "paid";
    private static final String PAY_RECORD_STATUS_CREATED = "created";
    private static final String PAY_RECORD_STATUS_PAYING = "paying";
    private static final String PAY_RECORD_STATUS_SUCCESS = "success";
    private static final String PAY_CALLBACK_STATUS_RECEIVED = "received";
    private static final String PAY_CALLBACK_STATUS_PROCESSED = "processed";
    private static final String DELIVERY_STATUS_PENDING = "pending";
    private static final String DELIVERY_STATUS_PREPARING = "preparing";
    private static final String DELIVERY_STATUS_SHIPPED = "shipped";
    private static final String DELIVERY_STATUS_RECEIVED = "received";
    private static final String ORDER_PAY_STATUS_REFUND_PARTIAL = "refund_partial";
    private static final String ORDER_PAY_STATUS_REFUND_FULL = "refund_full";
    private static final String REFUND_STATUS_CREATED = "created";
    private static final String REFUND_STATUS_PROCESSING = "processing";
    private static final String REFUND_STATUS_SUCCESS = "success";
    private static final String REFUND_STATUS_FAILED = "failed";
    private static final List<String> REFUND_OCCUPIED_STATUSES = List.of(REFUND_STATUS_PROCESSING, REFUND_STATUS_SUCCESS);
    private static final List<String> REFUND_SUCCESS_STATUSES = List.of(REFUND_STATUS_SUCCESS);
    private static final String REFUND_CALLBACK_STATUS_RECEIVED = "received";
    private static final String REFUND_CALLBACK_STATUS_PROCESSED = "processed";
    private static final String REFUND_CALLBACK_RESULT_SUCCESS = "success";
    private static final String REFUND_CALLBACK_RESULT_FAILED = "failed";
    private static final String AFTER_SALE_TYPE_REFUND_ONLY = "refund_only";
    private static final String AFTER_SALE_TYPE_RETURN_REFUND = "return_refund";
    private static final String AFTER_SALE_STATUS_PENDING_MERCHANT = "pending_merchant";
    private static final String AFTER_SALE_STATUS_AGREED = "agreed";
    private static final String AFTER_SALE_STATUS_REJECTED = "rejected";
    private static final String AFTER_SALE_STATUS_WAITING_RETURN = "waiting_return";
    private static final String AFTER_SALE_STATUS_REFUNDING = "refunding";
    private static final String AFTER_SALE_STATUS_COMPLETED = "completed";
    private static final String REVIEW_ACTION_AGREE = "agree";
    private static final String REVIEW_ACTION_REJECT = "reject";
    private static final String ITEM_REFUND_STATUS_NONE = "none";
    private static final String ITEM_REFUND_STATUS_PARTIAL_REFUNDING = "partial_refunding";
    private static final String ITEM_REFUND_STATUS_FULL_REFUNDING = "full_refunding";
    private static final String ITEM_REFUND_STATUS_PARTIAL_REFUNDED = "partial_refunded";
    private static final String ITEM_REFUND_STATUS_FULL_REFUNDED = "full_refunded";
    private static final String ITEM_REFUND_STATUS_REFUND_FAILED = "refund_failed";
    private static final String COMPENSATION_BIZ_TYPE_ORDER_REFUND = "order_refund";
    private static final String COMPENSATION_TYPE_REFUND_RETRY = "refund_retry";
    private static final String COMPENSATION_TASK_STATUS_PENDING = "pending";
    private static final int COMPENSATION_MAX_RETRY_COUNT = 10;

    private final OrderRepository repository;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public OrderApplicationService(OrderRepository repository,
                                   IdGenerator idGenerator,
                                   ObjectMapper objectMapper) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CartItemResponse upsertCartItem(CartItemUpsertRequest request) {
        var existingCartItem = repository.findCartItem(request.userId(), request.storeId(), request.skuId());
        CartItem item = existingCartItem
                .map(existing -> new CartItem(
                        existing.id(),
                        request.merchantId(),
                        request.storeId(),
                        request.userId(),
                        request.productId(),
                        request.skuId(),
                        request.quantity(),
                        request.selected(),
                        CART_STATUS_VALID,
                        request.snapshotPrice()))
                .orElseGet(() -> new CartItem(
                        idGenerator.nextId(),
                        request.merchantId(),
                        request.storeId(),
                        request.userId(),
                        request.productId(),
                        request.skuId(),
                        request.quantity(),
                        request.selected(),
                        CART_STATUS_VALID,
                        request.snapshotPrice()));
        if (existingCartItem.isPresent()) {
            repository.updateCartItem(item);
        } else {
            repository.createCartItem(item);
        }
        return toCartItemResponse(item);
    }

    public List<CartItemResponse> listCartItems(long merchantId, long storeId, long userId) {
        return repository.listCartItems(merchantId, storeId, userId).stream()
                .map(this::toCartItemResponse)
                .toList();
    }

    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) {
        List<CartItem> selectedItems = repository.listSelectedValidCartItems(request.merchantId(), request.storeId(), request.userId());
        if (selectedItems.isEmpty()) {
            throw new BusinessException(OrderErrorCode.CART_EMPTY);
        }
        Instant expiredAt = Instant.now().plusSeconds(15 * 60);
        BigDecimal totalAmount = calculateTotalAmount(selectedItems);
        String pricingSnapshotJson = buildPricingSnapshotJson(selectedItems, totalAmount);
        String cartItemIdsJson = writeJson(selectedItems.stream().map(CartItem::id).toList());
        String snapshotToken = "chk_" + idGenerator.nextId();
        CheckoutSnapshot snapshot = new CheckoutSnapshot(
                idGenerator.nextId(),
                request.merchantId(),
                request.storeId(),
                request.userId(),
                snapshotToken,
                cartItemIdsJson,
                pricingSnapshotJson,
                expiredAt);
        repository.createCheckoutSnapshot(snapshot);
        return new CheckoutResponse(snapshotToken, selectedItems.size(), totalAmount, expiredAt);
    }

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        CheckoutSnapshot snapshot = repository.findCheckoutSnapshotByToken(request.snapshotToken())
                .orElseThrow(() -> new BusinessException(OrderErrorCode.CHECKOUT_SNAPSHOT_NOT_FOUND));
        if (snapshot.expiredAt().isBefore(Instant.now())) {
            throw new BusinessException(OrderErrorCode.CHECKOUT_SNAPSHOT_EXPIRED);
        }
        if (snapshot.merchantId() != request.merchantId()
                || snapshot.storeId() != request.storeId()
                || snapshot.userId() != request.userId()) {
            throw new BusinessException(OrderErrorCode.CHECKOUT_SNAPSHOT_INVALID, "结算快照与请求主体不匹配");
        }
        SnapshotPricing pricing = parseSnapshotPricing(snapshot.pricingSnapshotJson());
        if (pricing.items().isEmpty()) {
            throw new BusinessException(OrderErrorCode.CHECKOUT_SNAPSHOT_INVALID, "结算快照中商品为空");
        }
        long orderId = idGenerator.nextId();
        String orderNo = "O" + orderId;
        OrderRecord order = new OrderRecord(
                orderId,
                orderNo,
                request.merchantId(),
                request.storeId(),
                request.userId(),
                snapshot.id(),
                ORDER_STATUS_PENDING_PAY,
                PAY_STATUS_UNPAID,
                null,
                trimToNull(request.buyerRemark()),
                pricing.totalAmount(),
                pricing.discountAmount(),
                pricing.payableAmount(),
                BigDecimal.ZERO,
                pricing.deliveryFeeAmount(),
                trimToNull(request.receiverName()),
                trimToNull(request.receiverMobile()),
                trimToNull(request.receiverAddress()),
                Instant.now(),
                null,
                null,
                null);

        List<OrderItemRecord> itemRecords = pricing.items().stream()
                .map(item -> new OrderItemRecord(
                        idGenerator.nextId(),
                        orderId,
                        request.merchantId(),
                        request.storeId(),
                        item.productId(),
                        item.skuId(),
                        item.productName(),
                        item.skuName(),
                        null,
                        item.salePrice(),
                        item.quantity(),
                        item.lineAmount(),
                        ITEM_REFUND_STATUS_NONE,
                        0,
                        BigDecimal.ZERO))
                .toList();

        repository.createOrder(order, itemRecords);
        repository.createStatusLog(new OrderStatusLogRecord(
                idGenerator.nextId(),
                orderId,
                orderNo,
                null,
                ORDER_STATUS_PENDING_PAY,
                "create_order",
                request.userId(),
                "用户" + request.userId(),
                "创建订单",
                Instant.now()));
        repository.markCartItemsDeleted(parseCartItemIds(snapshot.cartItemIdsJson()));
        return new CreateOrderResponse(orderNo, ORDER_STATUS_PENDING_PAY, PAY_STATUS_UNPAID, null, pricing.payableAmount());
    }

    @Transactional
    public OrderPayingResponse markOrderPaying(String orderNo, OrderPayingRequest request) {
        OrderRecord order = requireOrder(orderNo, request.merchantId(), request.storeId(), request.userId());
        if (!ORDER_STATUS_PENDING_PAY.equals(order.orderStatus())) {
            throw new BusinessException(OrderErrorCode.ORDER_STATUS_INVALID, "仅待支付订单可以发起支付");
        }
        if (PAY_STATUS_PAID.equals(order.payStatus())) {
            throw new BusinessException(OrderErrorCode.ORDER_ALREADY_PAID);
        }
        if (PAY_STATUS_PAYING.equals(order.payStatus())) {
            PayRecord existing = repository.findLatestPayRecordByOrderId(order.id())
                    .orElseThrow(() -> new BusinessException(OrderErrorCode.PAY_RECORD_NOT_FOUND, "订单支付状态异常，请重试"));
            if (PAY_RECORD_STATUS_PAYING.equals(existing.payStatus()) || PAY_RECORD_STATUS_CREATED.equals(existing.payStatus())) {
                return new OrderPayingResponse(order.orderNo(), existing.payNo(), PAY_STATUS_PAYING, existing.payAmount());
            }
        }
        if (!PAY_STATUS_UNPAID.equals(order.payStatus())) {
            throw new BusinessException(OrderErrorCode.ORDER_STATUS_INVALID, "当前支付状态不允许发起支付");
        }

        String payNo = "P" + idGenerator.nextId();
        PayRecord payRecord = new PayRecord(
                idGenerator.nextId(),
                payNo,
                order.id(),
                order.orderNo(),
                order.merchantId(),
                order.storeId(),
                normalize(request.payChannel()),
                trimToNull(request.payMethod()),
                PAY_RECORD_STATUS_PAYING,
                order.payableAmount());
        repository.createPayRecord(payRecord);
        boolean updated = repository.updateOrderPayStatusToPaying(order.id());
        if (!updated) {
            throw new BusinessException(OrderErrorCode.ORDER_STATUS_INVALID, "订单支付状态更新失败，请刷新后重试");
        }
        repository.createStatusLog(new OrderStatusLogRecord(
                idGenerator.nextId(),
                order.id(),
                order.orderNo(),
                order.orderStatus(),
                order.orderStatus(),
                "mark_paying",
                request.userId(),
                "用户" + request.userId(),
                "支付状态从 unpaid 更新为 paying",
                Instant.now()));
        return new OrderPayingResponse(order.orderNo(), payNo, PAY_STATUS_PAYING, order.payableAmount());
    }

    @Transactional
    public OrderLifecycleResponse markOrderPaid(String orderNo, OrderPaySuccessRequest request) {
        OrderRecord order = requireOrder(orderNo, request.merchantId(), request.storeId(), request.userId());
        if (PAY_STATUS_PAID.equals(order.payStatus())) {
            return toLifecycleResponse(order);
        }
        PayRecord payRecord = repository.findPayRecordByPayNo(request.payNo())
                .orElseThrow(() -> new BusinessException(OrderErrorCode.PAY_RECORD_NOT_FOUND));
        if (payRecord.orderId() != order.id()) {
            throw new BusinessException(OrderErrorCode.PAY_RECORD_NOT_FOUND, "支付流水与订单不匹配");
        }
        if (!PAY_RECORD_STATUS_CREATED.equals(payRecord.payStatus())
                && !PAY_RECORD_STATUS_PAYING.equals(payRecord.payStatus())
                && !PAY_RECORD_STATUS_SUCCESS.equals(payRecord.payStatus())) {
            throw new BusinessException(OrderErrorCode.ORDER_STATUS_INVALID, "支付流水状态不支持回写成功");
        }
        Instant paidAt = Instant.now();
        repository.updatePayRecordSuccess(
                payRecord.id(),
                trimToNull(request.thirdPartyTradeNo()),
                buildPaySuccessResponseJson(request, paidAt),
                paidAt);
        boolean updated = repository.updateOrderToPaid(order.id(), request.paidAmount(), paidAt);
        if (!updated) {
            OrderRecord latest = requireOrder(orderNo, request.merchantId(), request.storeId(), request.userId());
            if (PAY_STATUS_PAID.equals(latest.payStatus())) {
                return toLifecycleResponse(latest);
            }
            throw new BusinessException(OrderErrorCode.ORDER_STATUS_INVALID, "当前订单状态不允许回写支付成功");
        }
        repository.createStatusLog(new OrderStatusLogRecord(
                idGenerator.nextId(),
                order.id(),
                order.orderNo(),
                order.orderStatus(),
                ORDER_STATUS_PAID,
                "pay_success",
                request.userId(),
                "用户" + request.userId(),
                "支付成功",
                Instant.now()));
        return toLifecycleResponse(requireOrder(orderNo, request.merchantId(), request.storeId(), request.userId()));
    }

    @Transactional
    public OrderPayCallbackResponse handlePayCallback(OrderPayCallbackRequest request) {
        PayRecord payRecord = repository.findPayRecordByPayNo(request.payNo())
                .orElseThrow(() -> new BusinessException(OrderErrorCode.PAY_RECORD_NOT_FOUND));
        if (payRecord.merchantId() != request.merchantId() || payRecord.storeId() != request.storeId()) {
            throw new BusinessException(OrderErrorCode.PAY_RECORD_NOT_FOUND, "支付流水与商家或店铺不匹配");
        }
        OrderRecord order = requireOrderById(payRecord.orderId());
        if (order.merchantId() != request.merchantId() || order.storeId() != request.storeId()) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND, "订单与商家或店铺不匹配");
        }
        PayCallbackRecord callbackRecord = new PayCallbackRecord(
                idGenerator.nextId(),
                payRecord.id(),
                payRecord.payNo(),
                request.merchantId(),
                request.storeId(),
                normalize(request.payChannel()),
                trimToNull(request.thirdPartyTradeNo()),
                request.callbackEventId(),
                PAY_CALLBACK_STATUS_RECEIVED,
                null,
                null,
                trimToNull(request.callbackPayloadJson()),
                null);
        boolean inserted = repository.createPayCallbackRecordIfAbsent(callbackRecord);
        if (!inserted) {
            PayCallbackRecord existing = repository.findPayCallbackRecord(request.payNo(), request.callbackEventId())
                    .orElseThrow(() -> new BusinessException(OrderErrorCode.PAY_CALLBACK_STATUS_INVALID, "支付回调记录不存在"));
            OrderRecord latestOrder = requireOrderById(order.id());
            return new OrderPayCallbackResponse(
                    latestOrder.orderNo(),
                    request.payNo(),
                    latestOrder.orderStatus(),
                    latestOrder.payStatus(),
                    defaultText(existing.callbackStatus(), PAY_CALLBACK_STATUS_PROCESSED),
                    true);
        }

        Instant processedAt = Instant.now();
        if (!PAY_STATUS_PAID.equals(order.payStatus())) {
            if (!PAY_RECORD_STATUS_CREATED.equals(payRecord.payStatus())
                    && !PAY_RECORD_STATUS_PAYING.equals(payRecord.payStatus())
                    && !PAY_RECORD_STATUS_SUCCESS.equals(payRecord.payStatus())) {
                throw new BusinessException(OrderErrorCode.ORDER_STATUS_INVALID, "支付流水状态不支持回调处理");
            }
            repository.updatePayRecordSuccess(
                    payRecord.id(),
                    trimToNull(request.thirdPartyTradeNo()),
                    buildPayCallbackResponseJson(request, processedAt),
                    processedAt);
            boolean updated = repository.updateOrderToPaid(order.id(), request.paidAmount(), processedAt);
            if (!updated) {
                OrderRecord latestOrder = requireOrderById(order.id());
                if (!PAY_STATUS_PAID.equals(latestOrder.payStatus())) {
                    throw new BusinessException(OrderErrorCode.ORDER_STATUS_INVALID, "当前订单状态不允许回写支付成功");
                }
            } else {
                repository.createStatusLog(new OrderStatusLogRecord(
                        idGenerator.nextId(),
                        order.id(),
                        order.orderNo(),
                        order.orderStatus(),
                        ORDER_STATUS_PAID,
                        "pay_callback_success",
                        null,
                        "支付回调",
                        "支付回调处理成功",
                        Instant.now()));
            }
        }
        repository.markPayCallbackProcessed(
                callbackRecord.id(),
                PAY_CALLBACK_STATUS_PROCESSED,
                "success",
                null,
                processedAt);
        OrderRecord latestOrder = requireOrderById(order.id());
        return new OrderPayCallbackResponse(
                latestOrder.orderNo(),
                request.payNo(),
                latestOrder.orderStatus(),
                latestOrder.payStatus(),
                PAY_CALLBACK_STATUS_PROCESSED,
                false);
    }

    @Transactional
    public OrderRefundLifecycleResponse createRefund(String orderNo, OrderRefundCreateRequest request) {
        OrderRecord order = requireOrder(orderNo, request.merchantId(), request.storeId(), request.userId());
        if (!PAY_STATUS_PAID.equals(order.payStatus()) && !ORDER_PAY_STATUS_REFUND_PARTIAL.equals(order.payStatus())) {
            throw new BusinessException(OrderErrorCode.ORDER_STATUS_INVALID, "当前支付状态不允许发起退款");
        }
        BigDecimal paidAmount = order.paidAmount() != null ? order.paidAmount() : order.payableAmount();
        if (paidAmount == null || request.refundAmount() == null || request.refundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(OrderErrorCode.REFUND_AMOUNT_INVALID, "退款金额必须大于 0");
        }
        BigDecimal occupiedRefundAmount = repository.sumRefundAmountByOrderIdAndStatuses(order.id(), REFUND_OCCUPIED_STATUSES);
        BigDecimal remainingRefundAmount = paidAmount.subtract(occupiedRefundAmount);
        if (remainingRefundAmount.compareTo(BigDecimal.ZERO) <= 0
                || request.refundAmount().compareTo(remainingRefundAmount) > 0) {
            throw new BusinessException(OrderErrorCode.REFUND_AMOUNT_INVALID, "退款金额不能大于剩余可退金额");
        }
        PayRecord payRecord = repository.findLatestPayRecordByOrderId(order.id())
                .orElseThrow(() -> new BusinessException(OrderErrorCode.PAY_RECORD_NOT_FOUND, "未找到可关联的支付流水"));
        Instant appliedAt = Instant.now();
        String refundNo = "R" + idGenerator.nextId();
        RefundRecord refundRecord = new RefundRecord(
                idGenerator.nextId(),
                refundNo,
                order.id(),
                order.orderNo(),
                payRecord.id(),
                null,
                null,
                order.merchantId(),
                order.storeId(),
                trimToNull(request.refundReason()),
                REFUND_STATUS_PROCESSING,
                0,
                request.refundAmount(),
                null,
                appliedAt,
                null,
                null,
                null);
        repository.createRefundRecord(refundRecord);
        repository.updateOrderItemsRefundStatus(order.id(), REFUND_STATUS_PROCESSING);
        repository.createStatusLog(new OrderStatusLogRecord(
                idGenerator.nextId(),
                order.id(),
                order.orderNo(),
                order.orderStatus(),
                order.orderStatus(),
                "refund_apply",
                request.userId(),
                "用户" + request.userId(),
                StringUtils.hasText(request.refundReason()) ? request.refundReason().trim() : "发起退款申请",
                Instant.now()));
        return new OrderRefundLifecycleResponse(
                refundNo,
                order.orderNo(),
                REFUND_STATUS_PROCESSING,
                request.refundAmount(),
                order.payStatus(),
                null,
                false,
                null);
    }

    @Transactional
    public OrderRefundLifecycleResponse handleRefundCallback(String refundNo, OrderRefundCallbackRequest request) {
        RefundRecord refundRecord = repository.findRefundRecordByRefundNo(refundNo)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.REFUND_RECORD_NOT_FOUND));
        if (refundRecord.merchantId() != request.merchantId() || refundRecord.storeId() != request.storeId()) {
            throw new BusinessException(OrderErrorCode.REFUND_RECORD_NOT_FOUND, "退款流水与商家或店铺不匹配");
        }
        OrderRecord order = requireOrderById(refundRecord.orderId());
        String callbackResult = normalize(request.callbackStatus());
        if (!REFUND_CALLBACK_RESULT_SUCCESS.equals(callbackResult)
                && !REFUND_CALLBACK_RESULT_FAILED.equals(callbackResult)) {
            throw new BusinessException(OrderErrorCode.REFUND_STATUS_INVALID, "退款回调状态仅支持 success 或 failed");
        }
        RefundCallbackRecord callbackRecord = new RefundCallbackRecord(
                idGenerator.nextId(),
                refundRecord.id(),
                refundRecord.refundNo(),
                refundRecord.orderId(),
                refundRecord.merchantId(),
                refundRecord.storeId(),
                trimToNull(request.thirdPartyRefundNo()),
                request.callbackEventId(),
                REFUND_CALLBACK_STATUS_RECEIVED,
                null,
                null,
                trimToNull(request.callbackPayloadJson()),
                null);
        boolean inserted = repository.createRefundCallbackRecordIfAbsent(callbackRecord);
        if (!inserted) {
            RefundCallbackRecord existing = repository.findRefundCallbackRecord(refundNo, request.callbackEventId())
                    .orElseThrow(() -> new BusinessException(OrderErrorCode.REFUND_STATUS_INVALID, "退款回调记录不存在"));
            RefundRecord latestRefund = repository.findRefundRecordByRefundNo(refundNo)
                    .orElseThrow(() -> new BusinessException(OrderErrorCode.REFUND_RECORD_NOT_FOUND));
            OrderRecord latestOrder = requireOrderById(latestRefund.orderId());
            return new OrderRefundLifecycleResponse(
                    latestRefund.refundNo(),
                    latestRefund.orderNo(),
                    latestRefund.refundStatus(),
                    latestRefund.refundAmount(),
                    latestOrder.payStatus(),
                    defaultText(existing.callbackStatus(), REFUND_CALLBACK_STATUS_PROCESSED),
                    true,
                    latestRefund.compensationTaskCode());
        }

        Instant processedAt = Instant.now();
        String compensationTaskCode = refundRecord.compensationTaskCode();
        if (REFUND_CALLBACK_RESULT_SUCCESS.equals(callbackResult)) {
            boolean updated = repository.updateRefundRecordToSuccess(
                    refundRecord.id(),
                    trimToNull(request.thirdPartyRefundNo()),
                    processedAt);
            if (!updated) {
                RefundRecord latestRefund = repository.findRefundRecordByRefundNo(refundNo)
                        .orElseThrow(() -> new BusinessException(OrderErrorCode.REFUND_RECORD_NOT_FOUND));
                if (!REFUND_STATUS_SUCCESS.equals(latestRefund.refundStatus())) {
                    throw new BusinessException(OrderErrorCode.REFUND_STATUS_INVALID, "当前退款状态不允许回写成功");
                }
            }
            BigDecimal totalSuccessRefundAmount = repository.sumRefundAmountByOrderIdAndStatuses(order.id(), REFUND_SUCCESS_STATUSES);
            String targetPayStatus = resolveOrderPayStatusAfterRefund(order, totalSuccessRefundAmount);
            repository.updateOrderPayStatusAfterRefund(order.id(), targetPayStatus);
            if (refundRecord.orderItemId() != null) {
                OrderItemRecord orderItem = requireOrderItem(order.id(), refundRecord.orderItemId());
                refreshOrderItemRefundSnapshot(orderItem.id(), orderItem.quantity(), orderItem.lineAmount(), false);
                if (refundRecord.afterSaleId() != null) {
                    repository.updateAfterSaleStatus(
                            refundRecord.afterSaleId(),
                            AFTER_SALE_STATUS_COMPLETED,
                            null,
                            null,
                            processedAt,
                            null,
                            processedAt,
                            null);
                }
            } else {
                repository.updateOrderItemsRefundStatus(order.id(), REFUND_STATUS_SUCCESS);
            }
            repository.createStatusLog(new OrderStatusLogRecord(
                    idGenerator.nextId(),
                    order.id(),
                    order.orderNo(),
                    order.orderStatus(),
                    order.orderStatus(),
                    "refund_callback_success",
                    null,
                    "退款回调",
                    "退款回调成功",
                    Instant.now()));
            repository.markRefundCallbackProcessed(
                    callbackRecord.id(),
                    REFUND_CALLBACK_STATUS_PROCESSED,
                    REFUND_CALLBACK_RESULT_SUCCESS,
                    null,
                    processedAt);
        } else {
            String failureReason = StringUtils.hasText(request.failureReason())
                    ? request.failureReason().trim()
                    : "第三方退款失败";
            boolean updated = repository.updateRefundRecordToFailed(refundRecord.id(), failureReason);
            if (!updated) {
                RefundRecord latestRefund = repository.findRefundRecordByRefundNo(refundNo)
                        .orElseThrow(() -> new BusinessException(OrderErrorCode.REFUND_RECORD_NOT_FOUND));
                if (!REFUND_STATUS_FAILED.equals(latestRefund.refundStatus())) {
                    throw new BusinessException(OrderErrorCode.REFUND_STATUS_INVALID, "当前退款状态不允许回写失败");
                }
            }
            if (refundRecord.orderItemId() != null) {
                OrderItemRecord orderItem = requireOrderItem(order.id(), refundRecord.orderItemId());
                refreshOrderItemRefundSnapshot(orderItem.id(), orderItem.quantity(), orderItem.lineAmount(), false);
            } else {
                repository.updateOrderItemsRefundStatus(order.id(), REFUND_STATUS_FAILED);
            }
            compensationTaskCode = ensureRefundCompensationTask(refundRecord, order, failureReason, request.callbackEventId());
            repository.bindRefundCompensationTask(refundRecord.id(), compensationTaskCode);
            repository.createStatusLog(new OrderStatusLogRecord(
                    idGenerator.nextId(),
                    order.id(),
                    order.orderNo(),
                    order.orderStatus(),
                    order.orderStatus(),
                    "refund_callback_failed",
                    null,
                    "退款回调",
                    failureReason,
                    Instant.now()));
            repository.markRefundCallbackProcessed(
                    callbackRecord.id(),
                    REFUND_CALLBACK_STATUS_PROCESSED,
                    REFUND_CALLBACK_RESULT_FAILED,
                    failureReason,
                    processedAt);
        }

        RefundRecord latestRefund = repository.findRefundRecordByRefundNo(refundNo)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.REFUND_RECORD_NOT_FOUND));
        OrderRecord latestOrder = requireOrderById(latestRefund.orderId());
        return new OrderRefundLifecycleResponse(
                latestRefund.refundNo(),
                latestRefund.orderNo(),
                latestRefund.refundStatus(),
                latestRefund.refundAmount(),
                latestOrder.payStatus(),
                REFUND_CALLBACK_STATUS_PROCESSED,
                false,
                latestRefund.compensationTaskCode());
    }

    @Transactional
    public OrderAfterSaleLifecycleResponse createAfterSale(String orderNo, OrderAfterSaleCreateRequest request) {
        OrderRecord order = requireOrder(orderNo, request.merchantId(), request.storeId(), request.userId());
        validateAfterSalePayStatus(order);
        OrderItemRecord orderItem = requireOrderItem(order.id(), request.orderItemId());
        String afterSaleType = normalizeAfterSaleType(request.afterSaleType());
        validateAfterSaleRequest(order, orderItem, request.refundQuantity(), request.refundAmount());

        String afterSaleNo = "AS" + idGenerator.nextId();
        AfterSaleRecord afterSaleRecord = new AfterSaleRecord(
                idGenerator.nextId(),
                afterSaleNo,
                order.id(),
                order.orderNo(),
                orderItem.id(),
                order.merchantId(),
                order.storeId(),
                order.userId(),
                afterSaleType,
                AFTER_SALE_STATUS_PENDING_MERCHANT,
                trimToNull(request.reasonCode()),
                trimToNull(request.reasonDesc()),
                writeNullableJson(request.proofUrls()),
                request.refundQuantity(),
                request.refundAmount(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        repository.createAfterSaleRecord(afterSaleRecord);
        repository.createStatusLog(new OrderStatusLogRecord(
                idGenerator.nextId(),
                order.id(),
                order.orderNo(),
                order.orderStatus(),
                order.orderStatus(),
                "after_sale_apply",
                request.userId(),
                "用户" + request.userId(),
                defaultText(trimToNull(request.reasonDesc()), "发起售后申请"),
                Instant.now()));
        return toAfterSaleLifecycleResponse(afterSaleRecord, order.orderNo(), order.payStatus());
    }

    @Transactional
    public OrderAfterSaleLifecycleResponse reviewAfterSale(String afterSaleNo, OrderAfterSaleReviewRequest request) {
        AfterSaleRecord afterSale = requireAfterSale(afterSaleNo);
        if (afterSale.merchantId() != request.merchantId() || afterSale.storeId() != request.storeId()) {
            throw new BusinessException(OrderErrorCode.AFTER_SALE_NOT_FOUND);
        }
        if (!AFTER_SALE_STATUS_PENDING_MERCHANT.equals(afterSale.afterSaleStatus())) {
            throw new BusinessException(OrderErrorCode.AFTER_SALE_STATUS_INVALID, "当前售后状态不允许审核");
        }
        OrderRecord order = requireOrderById(afterSale.orderId());
        OrderItemRecord orderItem = requireOrderItem(order.id(), afterSale.orderItemId());
        String reviewAction = normalize(request.reviewAction());
        Instant now = Instant.now();
        if (REVIEW_ACTION_REJECT.equals(reviewAction)) {
            repository.updateAfterSaleStatus(
                    afterSale.id(),
                    AFTER_SALE_STATUS_REJECTED,
                    trimToNull(request.merchantResult()),
                    request.operatorId(),
                    now,
                    null,
                    null,
                    now);
            repository.createStatusLog(new OrderStatusLogRecord(
                    idGenerator.nextId(),
                    order.id(),
                    order.orderNo(),
                    order.orderStatus(),
                    order.orderStatus(),
                    "after_sale_reject",
                    request.operatorId(),
                    "商家" + request.operatorId(),
                    defaultText(trimToNull(request.merchantResult()), "商家拒绝售后"),
                    now));
            AfterSaleRecord latestAfterSale = requireAfterSale(afterSaleNo);
            return toAfterSaleLifecycleResponse(latestAfterSale, order.orderNo(), order.payStatus());
        }
        if (!REVIEW_ACTION_AGREE.equals(reviewAction)) {
            throw new BusinessException(OrderErrorCode.AFTER_SALE_STATUS_INVALID, "审核动作仅支持 agree 或 reject");
        }

        if (AFTER_SALE_TYPE_REFUND_ONLY.equals(afterSale.afterSaleType())) {
            RefundRecord refundRecord = createAfterSaleRefundRecord(order, orderItem, afterSale, now);
            repository.bindAfterSaleRefundRecord(afterSale.id(), refundRecord.id(), refundRecord.refundNo());
            repository.updateAfterSaleStatus(
                    afterSale.id(),
                    AFTER_SALE_STATUS_REFUNDING,
                    trimToNull(request.merchantResult()),
                    request.operatorId(),
                    now,
                    now,
                    null,
                    null);
            refreshOrderItemRefundSnapshot(orderItem.id(), orderItem.quantity(), orderItem.lineAmount(), true);
        } else {
            repository.updateAfterSaleStatus(
                    afterSale.id(),
                    AFTER_SALE_STATUS_WAITING_RETURN,
                    trimToNull(request.merchantResult()),
                    request.operatorId(),
                    now,
                    now,
                    null,
                    null);
        }
        repository.createStatusLog(new OrderStatusLogRecord(
                idGenerator.nextId(),
                order.id(),
                order.orderNo(),
                order.orderStatus(),
                order.orderStatus(),
                "after_sale_agree",
                request.operatorId(),
                "商家" + request.operatorId(),
                defaultText(trimToNull(request.merchantResult()), "商家同意售后"),
                now));
        AfterSaleRecord latestAfterSale = requireAfterSale(afterSaleNo);
        return toAfterSaleLifecycleResponse(latestAfterSale, order.orderNo(), order.payStatus());
    }

    @Transactional
    public OrderAfterSaleLifecycleResponse submitAfterSaleReturn(String afterSaleNo, OrderAfterSaleReturnRequest request) {
        AfterSaleRecord afterSale = requireAfterSale(afterSaleNo);
        if (afterSale.merchantId() != request.merchantId()
                || afterSale.storeId() != request.storeId()
                || afterSale.userId() != request.userId()) {
            throw new BusinessException(OrderErrorCode.AFTER_SALE_NOT_FOUND);
        }
        if (!AFTER_SALE_TYPE_RETURN_REFUND.equals(afterSale.afterSaleType())) {
            throw new BusinessException(OrderErrorCode.AFTER_SALE_TYPE_INVALID, "仅退货退款售后允许提交退货物流");
        }
        if (!AFTER_SALE_STATUS_WAITING_RETURN.equals(afterSale.afterSaleStatus())) {
            throw new BusinessException(OrderErrorCode.AFTER_SALE_STATUS_INVALID, "当前售后状态不允许提交退货物流");
        }
        OrderRecord order = requireOrderById(afterSale.orderId());
        OrderItemRecord orderItem = requireOrderItem(order.id(), afterSale.orderItemId());
        Instant now = Instant.now();
        repository.updateAfterSaleReturnInfo(
                afterSale.id(),
                trimToNull(request.returnCompany()),
                trimToNull(request.returnLogisticsNo()),
                now);
        RefundRecord refundRecord = createAfterSaleRefundRecord(order, orderItem, afterSale, now);
        repository.bindAfterSaleRefundRecord(afterSale.id(), refundRecord.id(), refundRecord.refundNo());
        repository.updateAfterSaleStatus(
                afterSale.id(),
                AFTER_SALE_STATUS_REFUNDING,
                afterSale.merchantResult(),
                request.userId(),
                now,
                afterSale.approvedAt(),
                null,
                null);
        refreshOrderItemRefundSnapshot(orderItem.id(), orderItem.quantity(), orderItem.lineAmount(), true);
        repository.createStatusLog(new OrderStatusLogRecord(
                idGenerator.nextId(),
                order.id(),
                order.orderNo(),
                order.orderStatus(),
                order.orderStatus(),
                "after_sale_return_submit",
                request.userId(),
                "用户" + request.userId(),
                "提交退货物流",
                now));
        AfterSaleRecord latestAfterSale = requireAfterSale(afterSaleNo);
        return toAfterSaleLifecycleResponse(latestAfterSale, order.orderNo(), order.payStatus());
    }

    @Transactional
    public OrderLifecycleResponse cancelOrder(String orderNo, OrderCancelRequest request) {
        OrderRecord order = requireOrder(orderNo, request.merchantId(), request.storeId(), request.userId());
        if (ORDER_STATUS_CANCELLED.equals(order.orderStatus())) {
            return toLifecycleResponse(order);
        }
        if (PAY_STATUS_PAID.equals(order.payStatus())) {
            throw new BusinessException(OrderErrorCode.ORDER_STATUS_INVALID, "已支付订单不允许取消");
        }
        if (!ORDER_STATUS_PENDING_PAY.equals(order.orderStatus())) {
            throw new BusinessException(OrderErrorCode.ORDER_STATUS_INVALID, "当前订单状态不允许取消");
        }
        boolean updated = repository.updateOrderToCancelled(order.id(), Instant.now());
        if (!updated) {
            OrderRecord latest = requireOrder(orderNo, request.merchantId(), request.storeId(), request.userId());
            if (ORDER_STATUS_CANCELLED.equals(latest.orderStatus())) {
                return toLifecycleResponse(latest);
            }
            throw new BusinessException(OrderErrorCode.ORDER_STATUS_INVALID, "订单取消失败，请刷新后重试");
        }
        repository.closePayRecordsByOrderId(order.id());
        repository.createStatusLog(new OrderStatusLogRecord(
                idGenerator.nextId(),
                order.id(),
                order.orderNo(),
                order.orderStatus(),
                ORDER_STATUS_CANCELLED,
                "cancel_order",
                request.userId(),
                "用户" + request.userId(),
                StringUtils.hasText(request.cancelReason()) ? request.cancelReason().trim() : "用户取消订单",
                Instant.now()));
        return toLifecycleResponse(requireOrder(orderNo, request.merchantId(), request.storeId(), request.userId()));
    }

    @Transactional
    public OrderLifecycleResponse updateDelivery(String orderNo, OrderDeliveryUpdateRequest request) {
        OrderRecord order = requireOrder(orderNo, request.merchantId(), request.storeId(), request.userId());
        String targetDeliveryStatus = normalize(request.deliveryStatus());
        String expectedOrderStatus;
        String expectedDeliveryStatus;
        String targetOrderStatus;
        Instant finishedAt = null;
        String defaultRemark;

        switch (targetDeliveryStatus) {
            case DELIVERY_STATUS_PREPARING -> {
                if (ORDER_STATUS_PREPARING.equals(order.orderStatus()) && DELIVERY_STATUS_PREPARING.equals(order.deliveryStatus())) {
                    return toLifecycleResponse(order);
                }
                if (!ORDER_STATUS_PAID.equals(order.orderStatus()) || !DELIVERY_STATUS_PENDING.equals(order.deliveryStatus())) {
                    throw new BusinessException(OrderErrorCode.ORDER_STATUS_INVALID, "仅已支付且待履约订单可进入备货");
                }
                expectedOrderStatus = ORDER_STATUS_PAID;
                expectedDeliveryStatus = DELIVERY_STATUS_PENDING;
                targetOrderStatus = ORDER_STATUS_PREPARING;
                defaultRemark = "订单开始备货";
            }
            case DELIVERY_STATUS_SHIPPED -> {
                if (ORDER_STATUS_SHIPPED.equals(order.orderStatus()) && DELIVERY_STATUS_SHIPPED.equals(order.deliveryStatus())) {
                    return toLifecycleResponse(order);
                }
                if (!ORDER_STATUS_PREPARING.equals(order.orderStatus()) || !DELIVERY_STATUS_PREPARING.equals(order.deliveryStatus())) {
                    throw new BusinessException(OrderErrorCode.ORDER_STATUS_INVALID, "仅备货中订单可更新为已发货");
                }
                expectedOrderStatus = ORDER_STATUS_PREPARING;
                expectedDeliveryStatus = DELIVERY_STATUS_PREPARING;
                targetOrderStatus = ORDER_STATUS_SHIPPED;
                defaultRemark = "订单已发货";
            }
            case DELIVERY_STATUS_RECEIVED -> {
                if (ORDER_STATUS_FINISHED.equals(order.orderStatus()) && DELIVERY_STATUS_RECEIVED.equals(order.deliveryStatus())) {
                    return toLifecycleResponse(order);
                }
                if (!ORDER_STATUS_SHIPPED.equals(order.orderStatus()) || !DELIVERY_STATUS_SHIPPED.equals(order.deliveryStatus())) {
                    throw new BusinessException(OrderErrorCode.ORDER_STATUS_INVALID, "仅已发货订单可确认收货");
                }
                expectedOrderStatus = ORDER_STATUS_SHIPPED;
                expectedDeliveryStatus = DELIVERY_STATUS_SHIPPED;
                targetOrderStatus = ORDER_STATUS_FINISHED;
                finishedAt = Instant.now();
                defaultRemark = "确认收货，订单完成";
            }
            default -> throw new BusinessException(OrderErrorCode.DELIVERY_STATUS_INVALID);
        }

        boolean updated = repository.updateOrderDeliveryStatus(
                order.id(),
                expectedOrderStatus,
                expectedDeliveryStatus,
                targetOrderStatus,
                targetDeliveryStatus,
                finishedAt);
        if (!updated) {
            OrderRecord latest = requireOrder(orderNo, request.merchantId(), request.storeId(), request.userId());
            if (targetOrderStatus.equals(latest.orderStatus()) && targetDeliveryStatus.equals(latest.deliveryStatus())) {
                return toLifecycleResponse(latest);
            }
            throw new BusinessException(OrderErrorCode.ORDER_STATUS_INVALID, "订单履约状态更新失败，请刷新后重试");
        }
        repository.createStatusLog(new OrderStatusLogRecord(
                idGenerator.nextId(),
                order.id(),
                order.orderNo(),
                order.orderStatus(),
                targetOrderStatus,
                "delivery_transition",
                request.userId(),
                "用户" + request.userId(),
                StringUtils.hasText(request.remark()) ? request.remark().trim() : defaultRemark,
                Instant.now()));
        return toLifecycleResponse(requireOrder(orderNo, request.merchantId(), request.storeId(), request.userId()));
    }

    public OrderDetailResponse getOrder(String orderNo, long merchantId, long storeId, long userId) {
        OrderRecord order = requireOrder(orderNo, merchantId, storeId, userId);
        List<OrderItemResponse> itemResponses = repository.listOrderItems(order.id()).stream()
                .map(this::toOrderItemResponse)
                .toList();
        List<OrderStatusLogResponse> statusLogs = repository.listOrderStatusLogs(order.id()).stream()
                .map(this::toOrderStatusLogResponse)
                .toList();
        return new OrderDetailResponse(
                order.id(),
                order.orderNo(),
                order.merchantId(),
                order.storeId(),
                order.userId(),
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
                order.createdAt(),
                order.paidAt(),
                order.cancelledAt(),
                order.finishedAt(),
                itemResponses,
                statusLogs);
    }

    public List<OrderSummaryResponse> listOrders(long merchantId, long storeId, Long userId, String orderStatus) {
        return repository.listOrders(merchantId, storeId, userId, orderStatus).stream()
                .map(this::toOrderSummaryResponse)
                .toList();
    }

    public List<OrderStatusLogResponse> listOrderStatusLogs(String orderNo, long merchantId, long storeId, long userId) {
        OrderRecord order = requireOrder(orderNo, merchantId, storeId, userId);
        return repository.listOrderStatusLogs(order.id()).stream()
                .map(this::toOrderStatusLogResponse)
                .toList();
    }

    private OrderRecord requireOrder(String orderNo, long merchantId, long storeId, long userId) {
        return repository.findOrderByOrderNo(orderNo, merchantId, storeId, userId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
    }

    private OrderRecord requireOrderById(long orderId) {
        return repository.findOrderById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
    }

    private AfterSaleRecord requireAfterSale(String afterSaleNo) {
        return repository.findAfterSaleByAfterSaleNo(afterSaleNo)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.AFTER_SALE_NOT_FOUND));
    }

    private OrderItemRecord requireOrderItem(long orderId, Long orderItemId) {
        if (orderItemId == null) {
            throw new BusinessException(OrderErrorCode.ORDER_ITEM_NOT_FOUND);
        }
        return repository.findOrderItem(orderId, orderItemId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_ITEM_NOT_FOUND));
    }

    private CartItemResponse toCartItemResponse(CartItem item) {
        return new CartItemResponse(
                item.id(),
                item.merchantId(),
                item.storeId(),
                item.userId(),
                item.productId(),
                item.skuId(),
                item.quantity(),
                item.selected(),
                item.invalidStatus(),
                item.snapshotPrice());
    }

    private OrderSummaryResponse toOrderSummaryResponse(OrderRecord order) {
        return new OrderSummaryResponse(
                order.orderNo(),
                order.merchantId(),
                order.storeId(),
                order.userId(),
                order.orderStatus(),
                order.payStatus(),
                order.deliveryStatus(),
                order.payableAmount(),
                order.paidAmount(),
                order.createdAt());
    }

    private OrderItemResponse toOrderItemResponse(OrderItemRecord item) {
        return new OrderItemResponse(
                item.id(),
                item.orderId(),
                item.productId(),
                item.skuId(),
                item.productName(),
                item.skuName(),
                item.salePrice(),
                item.quantity(),
                item.lineAmount(),
                item.refundStatus(),
                item.refundedQuantity(),
                item.refundedAmount());
    }

    private OrderStatusLogResponse toOrderStatusLogResponse(OrderStatusLogRecord log) {
        return new OrderStatusLogResponse(
                log.fromStatus(),
                log.toStatus(),
                log.operateType(),
                log.operatorId(),
                log.operatorName(),
                log.remark(),
                log.createdAt());
    }

    private OrderLifecycleResponse toLifecycleResponse(OrderRecord order) {
        return new OrderLifecycleResponse(order.orderNo(), order.orderStatus(), order.payStatus(), order.deliveryStatus());
    }

    private OrderAfterSaleLifecycleResponse toAfterSaleLifecycleResponse(AfterSaleRecord afterSale,
                                                                         String orderNo,
                                                                         String payStatus) {
        return new OrderAfterSaleLifecycleResponse(
                afterSale.afterSaleNo(),
                defaultText(afterSale.orderNo(), orderNo),
                afterSale.orderItemId(),
                afterSale.afterSaleType(),
                afterSale.afterSaleStatus(),
                afterSale.refundQuantity(),
                afterSale.refundAmount(),
                afterSale.refundNo(),
                payStatus);
    }

    private void validateAfterSalePayStatus(OrderRecord order) {
        if (!PAY_STATUS_PAID.equals(order.payStatus()) && !ORDER_PAY_STATUS_REFUND_PARTIAL.equals(order.payStatus())) {
            throw new BusinessException(OrderErrorCode.ORDER_STATUS_INVALID, "当前支付状态不允许发起售后");
        }
    }

    private void validateAfterSaleRequest(OrderRecord order,
                                          OrderItemRecord orderItem,
                                          int refundQuantity,
                                          BigDecimal refundAmount) {
        if (refundQuantity <= 0) {
            throw new BusinessException(OrderErrorCode.REFUND_ITEM_QUANTITY_INVALID, "退款数量必须大于 0");
        }
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(OrderErrorCode.REFUND_ITEM_AMOUNT_INVALID, "退款金额必须大于 0");
        }
        int occupiedRefundQuantity = repository.sumRefundQuantityByOrderItemIdAndStatuses(orderItem.id(), REFUND_OCCUPIED_STATUSES);
        BigDecimal occupiedRefundAmount = repository.sumRefundAmountByOrderItemIdAndStatuses(orderItem.id(), REFUND_OCCUPIED_STATUSES);
        if (refundQuantity > orderItem.quantity() - occupiedRefundQuantity) {
            throw new BusinessException(OrderErrorCode.REFUND_ITEM_QUANTITY_INVALID, "退款数量不能超过订单项剩余可退数量");
        }
        if (refundAmount.compareTo(orderItem.lineAmount().subtract(occupiedRefundAmount)) > 0) {
            throw new BusinessException(OrderErrorCode.REFUND_ITEM_AMOUNT_INVALID, "退款金额不能超过订单项剩余可退金额");
        }
        BigDecimal orderPaidAmount = order.paidAmount() != null ? order.paidAmount() : order.payableAmount();
        BigDecimal orderOccupiedRefundAmount = repository.sumRefundAmountByOrderIdAndStatuses(order.id(), REFUND_OCCUPIED_STATUSES);
        if (orderPaidAmount != null && refundAmount.compareTo(orderPaidAmount.subtract(orderOccupiedRefundAmount)) > 0) {
            throw new BusinessException(OrderErrorCode.REFUND_AMOUNT_INVALID, "退款金额不能大于订单剩余可退金额");
        }
    }

    private RefundRecord createAfterSaleRefundRecord(OrderRecord order,
                                                     OrderItemRecord orderItem,
                                                     AfterSaleRecord afterSale,
                                                     Instant appliedAt) {
        PayRecord payRecord = repository.findLatestPayRecordByOrderId(order.id())
                .orElseThrow(() -> new BusinessException(OrderErrorCode.PAY_RECORD_NOT_FOUND, "未找到可关联的支付流水"));
        String refundNo = "R" + idGenerator.nextId();
        RefundRecord refundRecord = new RefundRecord(
                idGenerator.nextId(),
                refundNo,
                order.id(),
                order.orderNo(),
                payRecord.id(),
                afterSale.id(),
                orderItem.id(),
                order.merchantId(),
                order.storeId(),
                defaultText(afterSale.reasonDesc(), afterSale.reasonCode()),
                REFUND_STATUS_PROCESSING,
                afterSale.refundQuantity(),
                afterSale.refundAmount(),
                null,
                appliedAt,
                null,
                null,
                null);
        repository.createRefundRecord(refundRecord);
        return refundRecord;
    }

    private void refreshOrderItemRefundSnapshot(long orderItemId,
                                                int totalQuantity,
                                                BigDecimal lineAmount,
                                                boolean includeOccupied) {
        int refundedQuantity = repository.sumRefundQuantityByOrderItemIdAndStatuses(orderItemId, REFUND_SUCCESS_STATUSES);
        BigDecimal refundedAmount = repository.sumRefundAmountByOrderItemIdAndStatuses(orderItemId, REFUND_SUCCESS_STATUSES);
        String refundStatus;
        if (includeOccupied) {
            int occupiedQuantity = repository.sumRefundQuantityByOrderItemIdAndStatuses(orderItemId, REFUND_OCCUPIED_STATUSES);
            BigDecimal occupiedAmount = repository.sumRefundAmountByOrderItemIdAndStatuses(orderItemId, REFUND_OCCUPIED_STATUSES);
            refundStatus = resolveProcessingItemRefundStatus(totalQuantity, lineAmount, occupiedQuantity, occupiedAmount);
        } else {
            refundStatus = resolveFinalItemRefundStatus(totalQuantity, lineAmount, refundedQuantity, refundedAmount);
        }
        repository.updateOrderItemRefundSnapshot(orderItemId, refundStatus, refundedQuantity, refundedAmount);
    }

    private String resolveProcessingItemRefundStatus(int totalQuantity,
                                                     BigDecimal lineAmount,
                                                     int occupiedQuantity,
                                                     BigDecimal occupiedAmount) {
        if (occupiedQuantity <= 0 && occupiedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ITEM_REFUND_STATUS_NONE;
        }
        return occupiedQuantity >= totalQuantity || occupiedAmount.compareTo(lineAmount) >= 0
                ? ITEM_REFUND_STATUS_FULL_REFUNDING
                : ITEM_REFUND_STATUS_PARTIAL_REFUNDING;
    }

    private String resolveFinalItemRefundStatus(int totalQuantity,
                                                BigDecimal lineAmount,
                                                int refundedQuantity,
                                                BigDecimal refundedAmount) {
        if (refundedQuantity <= 0 && refundedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ITEM_REFUND_STATUS_REFUND_FAILED;
        }
        return refundedQuantity >= totalQuantity || refundedAmount.compareTo(lineAmount) >= 0
                ? ITEM_REFUND_STATUS_FULL_REFUNDED
                : ITEM_REFUND_STATUS_PARTIAL_REFUNDED;
    }

    private BigDecimal calculateTotalAmount(List<CartItem> selectedItems) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem selectedItem : selectedItems) {
            BigDecimal lineAmount = selectedItem.snapshotPrice().multiply(BigDecimal.valueOf(selectedItem.quantity()));
            total = total.add(lineAmount);
        }
        return total;
    }

    private String buildPricingSnapshotJson(List<CartItem> selectedItems, BigDecimal totalAmount) {
        List<Map<String, Object>> itemSnapshots = new ArrayList<>();
        for (CartItem selectedItem : selectedItems) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("product_id", selectedItem.productId());
            item.put("sku_id", selectedItem.skuId());
            item.put("quantity", selectedItem.quantity());
            item.put("sale_price", selectedItem.snapshotPrice());
            item.put("line_amount", selectedItem.snapshotPrice().multiply(BigDecimal.valueOf(selectedItem.quantity())));
            item.put("product_name", "商品#" + selectedItem.productId());
            item.put("sku_name", "SKU#" + selectedItem.skuId());
            itemSnapshots.add(item);
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("schema_version", "v1");
        snapshot.put("item_count", itemSnapshots.size());
        snapshot.put("items", itemSnapshots);
        snapshot.put("total_amount", totalAmount);
        snapshot.put("discount_amount", BigDecimal.ZERO);
        snapshot.put("payable_amount", totalAmount);
        snapshot.put("delivery_fee_amount", BigDecimal.ZERO);
        return writeJson(snapshot);
    }

    private SnapshotPricing parseSnapshotPricing(String pricingSnapshotJson) {
        JsonNode root = readJson(pricingSnapshotJson);
        JsonNode itemsNode = root.path("items");
        if (!itemsNode.isArray() || itemsNode.isEmpty()) {
            throw new BusinessException(OrderErrorCode.CHECKOUT_SNAPSHOT_INVALID, "结算快照中商品明细缺失");
        }
        List<SnapshotItem> items = new ArrayList<>();
        for (JsonNode itemNode : itemsNode) {
            long productId = readLong(itemNode, "product_id");
            long skuId = readLong(itemNode, "sku_id");
            int quantity = readInt(itemNode, "quantity");
            if (quantity <= 0) {
                throw new BusinessException(OrderErrorCode.CHECKOUT_SNAPSHOT_INVALID, "结算快照中商品数量非法");
            }
            BigDecimal salePrice = readDecimal(itemNode, "sale_price");
            BigDecimal lineAmount = readDecimal(itemNode, "line_amount");
            String productName = defaultText(itemNode.path("product_name"), "商品#" + productId);
            String skuName = defaultText(itemNode.path("sku_name"), "SKU#" + skuId);
            items.add(new SnapshotItem(productId, skuId, quantity, salePrice, lineAmount, productName, skuName));
        }
        return new SnapshotPricing(
                items,
                readDecimal(root, "total_amount"),
                readDecimal(root, "discount_amount"),
                readDecimal(root, "payable_amount"),
                readDecimal(root, "delivery_fee_amount"));
    }

    private List<Long> parseCartItemIds(String cartItemIdsJson) {
        JsonNode node = readJson(cartItemIdsJson);
        if (!node.isArray()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.canConvertToLong()) {
                ids.add(item.longValue());
            }
        }
        return ids;
    }

    private String buildPaySuccessResponseJson(OrderPaySuccessRequest request, Instant paidAt) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("pay_no", request.payNo());
        response.put("third_party_trade_no", trimToNull(request.thirdPartyTradeNo()));
        response.put("paid_amount", request.paidAmount());
        response.put("paid_at", paidAt.toString());
        return writeJson(response);
    }

    private String buildPayCallbackResponseJson(OrderPayCallbackRequest request, Instant processedAt) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("pay_no", request.payNo());
        response.put("callback_event_id", request.callbackEventId());
        response.put("paid_amount", request.paidAmount());
        response.put("third_party_trade_no", trimToNull(request.thirdPartyTradeNo()));
        response.put("pay_channel", normalize(request.payChannel()));
        response.put("callback_payload_json", trimToNull(request.callbackPayloadJson()));
        response.put("processed_at", processedAt.toString());
        return writeJson(response);
    }

    private String normalizeAfterSaleType(String raw) {
        String normalized = normalize(raw);
        if (!AFTER_SALE_TYPE_REFUND_ONLY.equals(normalized) && !AFTER_SALE_TYPE_RETURN_REFUND.equals(normalized)) {
            throw new BusinessException(OrderErrorCode.AFTER_SALE_TYPE_INVALID);
        }
        return normalized;
    }

    private String resolveOrderPayStatusAfterRefund(OrderRecord order, BigDecimal totalSuccessRefundAmount) {
        BigDecimal paidAmount = order.paidAmount() != null ? order.paidAmount() : order.payableAmount();
        if (paidAmount == null || totalSuccessRefundAmount == null) {
            return ORDER_PAY_STATUS_REFUND_PARTIAL;
        }
        return totalSuccessRefundAmount.compareTo(paidAmount) >= 0
                ? ORDER_PAY_STATUS_REFUND_FULL
                : ORDER_PAY_STATUS_REFUND_PARTIAL;
    }

    private String ensureRefundCompensationTask(RefundRecord refundRecord,
                                                OrderRecord order,
                                                String failureReason,
                                                String callbackEventId) {
        return repository.findCompensationTask(
                        COMPENSATION_BIZ_TYPE_ORDER_REFUND,
                        refundRecord.refundNo(),
                        COMPENSATION_TYPE_REFUND_RETRY)
                .map(CompensationTaskRecord::taskCode)
                .orElseGet(() -> createRefundCompensationTask(refundRecord, order, failureReason, callbackEventId));
    }

    private String createRefundCompensationTask(RefundRecord refundRecord,
                                                OrderRecord order,
                                                String failureReason,
                                                String callbackEventId) {
        String taskCode = "CT" + idGenerator.nextId();
        Instant nextRetryAt = Instant.now().plusSeconds(5 * 60);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("refund_no", refundRecord.refundNo());
        payload.put("order_no", refundRecord.orderNo());
        payload.put("order_id", refundRecord.orderId());
        payload.put("merchant_id", order.merchantId());
        payload.put("store_id", order.storeId());
        payload.put("callback_event_id", trimToNull(callbackEventId));
        payload.put("failure_reason", trimToNull(failureReason));
        payload.put("refund_amount", refundRecord.refundAmount());

        CompensationTaskRecord taskRecord = new CompensationTaskRecord(
                idGenerator.nextId(),
                taskCode,
                COMPENSATION_BIZ_TYPE_ORDER_REFUND,
                refundRecord.refundNo(),
                COMPENSATION_TYPE_REFUND_RETRY,
                COMPENSATION_TASK_STATUS_PENDING,
                0,
                COMPENSATION_MAX_RETRY_COUNT,
                nextRetryAt,
                null,
                trimToNull(failureReason),
                writeJson(payload),
                null,
                order.merchantId(),
                order.merchantId());
        boolean created = repository.createCompensationTaskIfAbsent(taskRecord);
        if (created) {
            return taskCode;
        }
        return repository.findCompensationTask(
                        COMPENSATION_BIZ_TYPE_ORDER_REFUND,
                        refundRecord.refundNo(),
                        COMPENSATION_TYPE_REFUND_RETRY)
                .map(CompensationTaskRecord::taskCode)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.REFUND_STATUS_INVALID, "退款补偿任务创建失败"));
    }

    private JsonNode readJson(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(OrderErrorCode.CHECKOUT_SNAPSHOT_INVALID);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(OrderErrorCode.CHECKOUT_SNAPSHOT_INVALID);
        }
    }

    private String writeNullableJson(Object value) {
        if (value == null) {
            return null;
        }
        return writeJson(value);
    }

    private long readLong(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (!value.canConvertToLong()) {
            throw new BusinessException(OrderErrorCode.CHECKOUT_SNAPSHOT_INVALID, "结算快照字段 " + fieldName + " 非法");
        }
        return value.longValue();
    }

    private int readInt(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (!value.canConvertToInt()) {
            throw new BusinessException(OrderErrorCode.CHECKOUT_SNAPSHOT_INVALID, "结算快照字段 " + fieldName + " 非法");
        }
        return value.intValue();
    }

    private BigDecimal readDecimal(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            throw new BusinessException(OrderErrorCode.CHECKOUT_SNAPSHOT_INVALID, "结算快照字段 " + fieldName + " 缺失");
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        if (value.isTextual()) {
            try {
                return new BigDecimal(value.textValue());
            } catch (NumberFormatException ex) {
                throw new BusinessException(OrderErrorCode.CHECKOUT_SNAPSHOT_INVALID, "结算快照字段 " + fieldName + " 非法");
            }
        }
        throw new BusinessException(OrderErrorCode.CHECKOUT_SNAPSHOT_INVALID, "结算快照字段 " + fieldName + " 非法");
    }

    private static String defaultText(JsonNode value, String defaultValue) {
        if (value == null || value.isNull() || !StringUtils.hasText(value.asText())) {
            return defaultValue;
        }
        return value.asText().trim();
    }

    private static String defaultText(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.trim();
    }

    private record SnapshotPricing(
            List<SnapshotItem> items,
            BigDecimal totalAmount,
            BigDecimal discountAmount,
            BigDecimal payableAmount,
            BigDecimal deliveryFeeAmount) {
    }

    private record SnapshotItem(
            long productId,
            long skuId,
            int quantity,
            BigDecimal salePrice,
            BigDecimal lineAmount,
            String productName,
            String skuName) {
    }
}
