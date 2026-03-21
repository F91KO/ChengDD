package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record OrderAfterSaleLifecycleResponse(
        @JsonProperty("after_sale_no")
        String afterSaleNo,
        @JsonProperty("order_no")
        String orderNo,
        @JsonProperty("order_item_id")
        Long orderItemId,
        @JsonProperty("after_sale_type")
        String afterSaleType,
        @JsonProperty("after_sale_status")
        String afterSaleStatus,
        @JsonProperty("refund_quantity")
        Integer refundQuantity,
        @JsonProperty("refund_amount")
        BigDecimal refundAmount,
        @JsonProperty("refund_no")
        String refundNo,
        @JsonProperty("pay_status")
        String payStatus) {
}
