package com.cdd.api.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record FeatureSwitchStatusChangeRequest(
        @JsonProperty("status")
        @NotBlank(message = "状态不能为空")
        String status) {
}
