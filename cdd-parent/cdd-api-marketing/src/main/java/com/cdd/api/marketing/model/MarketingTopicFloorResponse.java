package com.cdd.api.marketing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record MarketingTopicFloorResponse(
        @JsonProperty("id")
        Long id,
        @JsonProperty("merchant_id")
        Long merchantId,
        @JsonProperty("store_id")
        Long storeId,
        @JsonProperty("topic_name")
        String topicName,
        @JsonProperty("topic_code")
        String topicCode,
        @JsonProperty("banner_url")
        String bannerUrl,
        @JsonProperty("floor_payload")
        JsonNode floorPayload,
        @JsonProperty("status")
        String status) {
}
