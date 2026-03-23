package com.cdd.api.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ReportOrderDailyResponse(
        @JsonProperty("id")
        Long id,
        @JsonProperty("merchant_id")
        Long merchantId,
        @JsonProperty("store_id")
        Long storeId,
        @JsonProperty("stat_date")
        String statDate,
        @JsonProperty("order_count")
        Long orderCount,
        @JsonProperty("paid_order_count")
        Long paidOrderCount,
        @JsonProperty("gross_amount")
        BigDecimal grossAmount,
        @JsonProperty("refund_amount")
        BigDecimal refundAmount) {
}
