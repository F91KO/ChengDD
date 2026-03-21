package com.cdd.api.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record FeatureSwitchUpsertRequest(
        @JsonProperty("switch_code")
        @NotBlank(message = "开关编码不能为空")
        String switchCode,
        @JsonProperty("switch_name")
        @NotBlank(message = "开关名称不能为空")
        String switchName,
        @JsonProperty("switch_scope")
        @NotBlank(message = "开关范围不能为空")
        String switchScope,
        @JsonProperty("default_value")
        @NotBlank(message = "默认值不能为空")
        String defaultValue,
        @JsonProperty("status")
        @NotBlank(message = "状态不能为空")
        String status) {
}
