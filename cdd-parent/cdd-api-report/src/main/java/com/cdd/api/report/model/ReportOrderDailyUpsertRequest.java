package com.cdd.api.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ReportOrderDailyUpsertRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("stat_date")
        @NotBlank(message = "统计日期不能为空")
        String statDate,
        @JsonProperty("order_count")
        @NotNull(message = "订单数不能为空")
        Long orderCount,
        @JsonProperty("paid_order_count")
        @NotNull(message = "支付订单数不能为空")
        Long paidOrderCount,
        @JsonProperty("gross_amount")
        @NotNull(message = "成交总额不能为空")
        BigDecimal grossAmount,
        @JsonProperty("refund_amount")
        @NotNull(message = "退款总额不能为空")
        BigDecimal refundAmount) {
}
