package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record OrderPayingResponse(
        @JsonProperty("order_no")
        String orderNo,
        @JsonProperty("pay_no")
        String payNo,
        @JsonProperty("pay_status")
        String payStatus,
        @JsonProperty("pay_amount")
        BigDecimal payAmount) {
}
