package com.cdd.api.marketing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record MarketingRecommendRuleResponse(
        @JsonProperty("id")
        Long id,
        @JsonProperty("merchant_id")
        Long merchantId,
        @JsonProperty("store_id")
        Long storeId,
        @JsonProperty("rule_name")
        String ruleName,
        @JsonProperty("scene_code")
        String sceneCode,
        @JsonProperty("rule_payload")
        JsonNode rulePayload,
        @JsonProperty("status")
        String status) {
}
