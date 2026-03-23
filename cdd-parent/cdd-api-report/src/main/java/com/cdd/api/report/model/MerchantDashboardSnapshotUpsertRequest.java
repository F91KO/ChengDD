package com.cdd.api.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MerchantDashboardSnapshotUpsertRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("snapshot_time")
        @NotBlank(message = "快照时间不能为空")
        String snapshotTime,
        @JsonProperty("dashboard_payload")
        @NotNull(message = "看板快照不能为空")
        JsonNode dashboardPayload) {
}
