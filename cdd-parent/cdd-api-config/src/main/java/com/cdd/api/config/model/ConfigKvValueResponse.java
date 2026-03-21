package com.cdd.api.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConfigKvValueResponse(
        @JsonProperty("config_group")
        String configGroup,
        @JsonProperty("config_key")
        String configKey,
        @JsonProperty("config_value")
        String configValue,
        @JsonProperty("source")
        String source,
        @JsonProperty("merchant_id")
        String merchantId) {
}
