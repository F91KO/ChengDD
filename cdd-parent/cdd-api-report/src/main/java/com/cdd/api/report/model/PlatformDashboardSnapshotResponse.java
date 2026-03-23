package com.cdd.api.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record PlatformDashboardSnapshotResponse(
        @JsonProperty("id")
        Long id,
        @JsonProperty("snapshot_time")
        String snapshotTime,
        @JsonProperty("dashboard_payload")
        JsonNode dashboardPayload) {
}
