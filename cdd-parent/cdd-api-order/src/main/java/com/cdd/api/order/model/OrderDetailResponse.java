package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetailResponse(
        long id,
        @JsonProperty("order_no")
        String orderNo,
        @JsonProperty("merchant_id")
        long merchantId,
        @JsonProperty("store_id")
        long storeId,
        @JsonProperty("user_id")
        long userId,
        @JsonProperty("order_status")
        String orderStatus,
        @JsonProperty("pay_status")
        String payStatus,
        @JsonProperty("delivery_status")
        String deliveryStatus,
        @JsonProperty("buyer_remark")
        String buyerRemark,
        @JsonProperty("total_amount")
        BigDecimal totalAmount,
        @JsonProperty("discount_amount")
        BigDecimal discountAmount,
        @JsonProperty("payable_amount")
        BigDecimal payableAmount,
        @JsonProperty("paid_amount")
        BigDecimal paidAmount,
        @JsonProperty("delivery_fee_amount")
        BigDecimal deliveryFeeAmount,
        @JsonProperty("receiver_name")
        String receiverName,
        @JsonProperty("receiver_mobile")
        String receiverMobile,
        @JsonProperty("receiver_address")
        String receiverAddress,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("paid_at")
        Instant paidAt,
        @JsonProperty("cancelled_at")
        Instant cancelledAt,
        @JsonProperty("finished_at")
        Instant finishedAt,
        @JsonProperty("items")
        List<OrderItemResponse> items,
        @JsonProperty("status_logs")
        List<OrderStatusLogResponse> statusLogs) {
}
