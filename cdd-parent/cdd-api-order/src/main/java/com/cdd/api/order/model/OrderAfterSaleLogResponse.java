package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record OrderAfterSaleLogResponse(
        @JsonProperty("log_type")
        String logType,
        @JsonProperty("after_sale_status")
        String afterSaleStatus,
        @JsonProperty("operator_id")
        Long operatorId,
        @JsonProperty("operator_name")
        String operatorName,
        @JsonProperty("message")
        String message,
        @JsonProperty("created_at")
        Instant createdAt) {
}
