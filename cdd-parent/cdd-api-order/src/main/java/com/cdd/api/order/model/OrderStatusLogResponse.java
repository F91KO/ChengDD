package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record OrderStatusLogResponse(
        @JsonProperty("from_status")
        String fromStatus,
        @JsonProperty("to_status")
        String toStatus,
        @JsonProperty("operate_type")
        String operateType,
        @JsonProperty("operator_id")
        Long operatorId,
        @JsonProperty("operator_name")
        String operatorName,
        @JsonProperty("remark")
        String remark,
        @JsonProperty("created_at")
        Instant createdAt) {
}
