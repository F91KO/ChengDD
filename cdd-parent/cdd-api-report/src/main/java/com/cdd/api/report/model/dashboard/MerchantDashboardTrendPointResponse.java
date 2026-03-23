package com.cdd.api.report.model.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record MerchantDashboardTrendPointResponse(
        @JsonProperty("stat_date")
        String statDate,
        @JsonProperty("gross_amount")
        BigDecimal grossAmount,
        @JsonProperty("order_count")
        Long orderCount,
        @JsonProperty("visitor_count")
        Long visitorCount,
        @JsonProperty("click_count")
        Long clickCount) {
}
