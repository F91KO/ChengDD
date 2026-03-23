package com.cdd.api.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConfigPublishStepResponse(
        @JsonProperty("step_code")
        String stepCode,
        @JsonProperty("step_name")
        String stepName,
        @JsonProperty("step_order")
        Integer stepOrder,
        @JsonProperty("step_status")
        String stepStatus,
        @JsonProperty("result_message")
        String resultMessage,
        @JsonProperty("error_code")
        String errorCode,
        @JsonProperty("retry_count")
        Integer retryCount,
        @JsonProperty("started_at")
        String startedAt,
        @JsonProperty("finished_at")
        String finishedAt) {
}
