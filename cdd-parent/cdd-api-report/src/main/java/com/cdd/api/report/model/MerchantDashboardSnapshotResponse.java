package com.cdd.api.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record MerchantDashboardSnapshotResponse(
        @JsonProperty("id")
        Long id,
        @JsonProperty("merchant_id")
        Long merchantId,
        @JsonProperty("store_id")
        Long storeId,
        @JsonProperty("snapshot_time")
        String snapshotTime,
        @JsonProperty("dashboard_payload")
        JsonNode dashboardPayload) {
}
