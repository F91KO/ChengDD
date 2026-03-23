package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryResponse(
        @JsonProperty("order_no")
        String orderNo,
        @JsonProperty("merchant_id")
        long merchantId,
        @JsonProperty("store_id")
        long storeId,
        @JsonProperty("user_id")
        long userId,
        @JsonProperty("customer_identifier")
        String customerIdentifier,
        @JsonProperty("channel")
        String channel,
        @JsonProperty("product_summary")
        String productSummary,
        @JsonProperty("order_status")
        String orderStatus,
        @JsonProperty("pay_status")
        String payStatus,
        @JsonProperty("delivery_status")
        String deliveryStatus,
        @JsonProperty("payable_amount")
        BigDecimal payableAmount,
        @JsonProperty("paid_amount")
        BigDecimal paidAmount,
        @JsonProperty("created_at")
        Instant createdAt) {
}
