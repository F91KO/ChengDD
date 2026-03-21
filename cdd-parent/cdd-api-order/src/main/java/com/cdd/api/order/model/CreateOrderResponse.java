package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record CreateOrderResponse(
        @JsonProperty("order_no")
        String orderNo,
        @JsonProperty("order_status")
        String orderStatus,
        @JsonProperty("pay_status")
        String payStatus,
        @JsonProperty("delivery_status")
        String deliveryStatus,
        @JsonProperty("payable_amount")
        BigDecimal payableAmount) {
}
