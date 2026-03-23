package com.cdd.api.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ConfigPublishSnapshotResponse(
        @JsonProperty("platform_configs")
        List<ConfigPublishConfigValueResponse> platformConfigs,
        @JsonProperty("merchant_overrides")
        List<ConfigPublishConfigValueResponse> merchantOverrides,
        @JsonProperty("platform_feature_switches")
        List<FeatureSwitchValueResponse> platformFeatureSwitches,
        @JsonProperty("merchant_feature_switches")
        List<FeatureSwitchValueResponse> merchantFeatureSwitches) {
}
