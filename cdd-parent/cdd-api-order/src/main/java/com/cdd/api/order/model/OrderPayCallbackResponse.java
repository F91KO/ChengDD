package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderPayCallbackResponse(
        @JsonProperty("order_no")
        String orderNo,
        @JsonProperty("pay_no")
        String payNo,
        @JsonProperty("order_status")
        String orderStatus,
        @JsonProperty("pay_status")
        String payStatus,
        @JsonProperty("callback_status")
        String callbackStatus,
        @JsonProperty("duplicated")
        boolean duplicated) {
}
