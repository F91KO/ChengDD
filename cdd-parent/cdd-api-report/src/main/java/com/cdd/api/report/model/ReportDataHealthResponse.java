package com.cdd.api.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ReportDataHealthResponse(
        @JsonProperty("status")
        String status,
        @JsonProperty("checked_at")
        String checkedAt,
        @JsonProperty("home_event_daily_count")
        Long homeEventDailyCount,
        @JsonProperty("order_daily_count")
        Long orderDailyCount,
        @JsonProperty("product_daily_count")
        Long productDailyCount,
        @JsonProperty("merchant_dashboard_snapshot_count")
        Long merchantDashboardSnapshotCount,
        @JsonProperty("platform_dashboard_snapshot_count")
        Long platformDashboardSnapshotCount,
        @JsonProperty("latest_home_event_stat_date")
        String latestHomeEventStatDate,
        @JsonProperty("latest_order_stat_date")
        String latestOrderStatDate,
        @JsonProperty("latest_product_stat_date")
        String latestProductStatDate,
        @JsonProperty("latest_merchant_dashboard_snapshot_time")
        String latestMerchantDashboardSnapshotTime,
        @JsonProperty("latest_platform_dashboard_snapshot_time")
        String latestPlatformDashboardSnapshotTime,
        @JsonProperty("issues")
        List<String> issues) {
}
