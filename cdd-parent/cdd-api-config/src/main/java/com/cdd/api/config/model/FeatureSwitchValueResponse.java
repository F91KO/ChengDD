package com.cdd.api.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FeatureSwitchValueResponse(
        @JsonProperty("switch_code")
        String switchCode,
        @JsonProperty("switch_name")
        String switchName,
        @JsonProperty("switch_scope")
        String switchScope,
        @JsonProperty("default_value")
        String defaultValue,
        @JsonProperty("effective_value")
        String effectiveValue,
        @JsonProperty("status")
        String status,
        @JsonProperty("source")
        String source,
        @JsonProperty("merchant_id")
        String merchantId) {
}
