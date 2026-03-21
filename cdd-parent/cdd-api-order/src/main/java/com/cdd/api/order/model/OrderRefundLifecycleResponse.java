package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record OrderRefundLifecycleResponse(
        @JsonProperty("refund_no")
        String refundNo,
        @JsonProperty("order_no")
        String orderNo,
        @JsonProperty("refund_status")
        String refundStatus,
        @JsonProperty("refund_amount")
        BigDecimal refundAmount,
        @JsonProperty("pay_status")
        String payStatus,
        @JsonProperty("callback_status")
        String callbackStatus,
        @JsonProperty("duplicated")
        boolean duplicated,
        @JsonProperty("compensation_task_code")
        String compensationTaskCode) {
}
