package com.cdd.api.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReportHealthItemResponse(
        String code,
        String name,
        String status,
        @JsonProperty("latest_data_time")
        String latestDataTime,
        String message) {
}
