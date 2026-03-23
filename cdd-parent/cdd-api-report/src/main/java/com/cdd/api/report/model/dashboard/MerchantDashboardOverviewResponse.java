package com.cdd.api.report.model.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record MerchantDashboardOverviewResponse(
        @JsonProperty("merchant_id")
        Long merchantId,
        @JsonProperty("store_id")
        Long storeId,
        @JsonProperty("snapshot_time")
        String snapshotTime,
        @JsonProperty("gmv")
        BigDecimal gmv,
        @JsonProperty("order_count")
        Long orderCount,
        @JsonProperty("paid_order_count")
        Long paidOrderCount,
        @JsonProperty("visitor_count")
        Long visitorCount,
        @JsonProperty("click_count")
        Long clickCount,
        @JsonProperty("active_product_count")
        Long activeProductCount,
        @JsonProperty("pending_delivery_count")
        Long pendingDeliveryCount,
        @JsonProperty("after_sale_processing_count")
        Long afterSaleProcessingCount,
        @JsonProperty("release_exception_count")
        Long releaseExceptionCount,
        @JsonProperty("todo_summary")
        List<DashboardTodoItemResponse> todoSummary,
        @JsonProperty("trend")
        List<MerchantDashboardTrendPointResponse> trend) {
}
