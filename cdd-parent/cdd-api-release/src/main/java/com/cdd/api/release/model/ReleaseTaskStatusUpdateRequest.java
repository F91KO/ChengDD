package com.cdd.api.release.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ReleaseTaskStatusUpdateRequest(
        @JsonProperty("target_status")
        @NotBlank(message = "目标状态不能为空")
        String targetStatus,
        @JsonProperty("current_step_code")
        String currentStepCode,
        @JsonProperty("error_code")
        String errorCode,
        @JsonProperty("error_message")
        String errorMessage) {
}
