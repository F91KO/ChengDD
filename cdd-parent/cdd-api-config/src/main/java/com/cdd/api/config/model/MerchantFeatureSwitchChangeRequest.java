package com.cdd.api.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record MerchantFeatureSwitchChangeRequest(
        @JsonProperty("merchant_id")
        @NotBlank(message = "商家标识不能为空")
        String merchantId,
        @JsonProperty("switch_value")
        @NotBlank(message = "开关值不能为空")
        String switchValue) {
}
