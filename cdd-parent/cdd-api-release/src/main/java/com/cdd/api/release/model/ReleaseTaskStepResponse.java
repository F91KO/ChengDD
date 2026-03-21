package com.cdd.api.release.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record ReleaseTaskStepResponse(
        @JsonProperty("step_code")
        String stepCode,
        @JsonProperty("step_name")
        String stepName,
        @JsonProperty("step_order")
        int stepOrder,
        @JsonProperty("step_status")
        String stepStatus,
        @JsonProperty("result_message")
        String resultMessage,
        @JsonProperty("error_code")
        String errorCode,
        @JsonProperty("retry_count")
        int retryCount,
        @JsonProperty("started_at")
        Instant startedAt,
        @JsonProperty("finished_at")
        Instant finishedAt) {
}
