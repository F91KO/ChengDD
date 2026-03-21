package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderLifecycleResponse(
        @JsonProperty("order_no")
        String orderNo,
        @JsonProperty("order_status")
        String orderStatus,
        @JsonProperty("pay_status")
        String payStatus,
        @JsonProperty("delivery_status")
        String deliveryStatus) {
}
