package com.cdd.api.report.model.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record PlatformDashboardOverviewResponse(
        @JsonProperty("snapshot_time")
        String snapshotTime,
        @JsonProperty("merchant_count")
        Long merchantCount,
        @JsonProperty("daily_active_store_count")
        Long dailyActiveStoreCount,
        @JsonProperty("today_order_count")
        Long todayOrderCount,
        @JsonProperty("today_gmv")
        BigDecimal todayGmv) {
}
