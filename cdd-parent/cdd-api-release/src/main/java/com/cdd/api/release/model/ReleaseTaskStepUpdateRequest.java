package com.cdd.api.release.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReleaseTaskStepUpdateRequest(
        @JsonProperty("step_code")
        String stepCode,
        @JsonProperty("step_name")
        @NotBlank(message = "步骤名称不能为空")
        String stepName,
        @JsonProperty("step_order")
        @NotNull(message = "步骤顺序不能为空")
        Integer stepOrder,
        @JsonProperty("step_status")
        @NotBlank(message = "步骤状态不能为空")
        String stepStatus,
        @JsonProperty("result_message")
        String resultMessage,
        @JsonProperty("error_code")
        String errorCode) {
}
