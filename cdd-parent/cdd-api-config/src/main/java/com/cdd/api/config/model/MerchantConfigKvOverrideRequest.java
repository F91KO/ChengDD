package com.cdd.api.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record MerchantConfigKvOverrideRequest(
        @JsonProperty("merchant_id")
        @NotBlank(message = "商家标识不能为空")
        String merchantId,
        @JsonProperty("config_group")
        @NotBlank(message = "配置分组不能为空")
        String configGroup,
        @JsonProperty("config_key")
        @NotBlank(message = "配置键不能为空")
        String configKey,
        @JsonProperty("config_value")
        @NotBlank(message = "配置值不能为空")
        String configValue) {
}
