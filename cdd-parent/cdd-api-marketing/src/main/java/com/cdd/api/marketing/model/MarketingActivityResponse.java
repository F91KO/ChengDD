package com.cdd.api.marketing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record MarketingActivityResponse(
        @JsonProperty("id")
        Long id,
        @JsonProperty("merchant_id")
        Long merchantId,
        @JsonProperty("store_id")
        Long storeId,
        @JsonProperty("activity_name")
        String activityName,
        @JsonProperty("activity_type")
        String activityType,
        @JsonProperty("activity_status")
        String activityStatus,
        @JsonProperty("start_at")
        String startAt,
        @JsonProperty("end_at")
        String endAt,
        @JsonProperty("rule_payload")
        JsonNode rulePayload,
        @JsonProperty("banner_url")
        String bannerUrl,
        @JsonProperty("products")
        List<ActivityProductResponse> products) {
}
