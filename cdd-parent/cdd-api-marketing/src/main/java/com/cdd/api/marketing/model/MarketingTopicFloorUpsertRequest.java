package com.cdd.api.marketing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MarketingTopicFloorUpsertRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("topic_name")
        @NotBlank(message = "专题会场名称不能为空")
        String topicName,
        @JsonProperty("topic_code")
        @NotBlank(message = "专题会场编码不能为空")
        String topicCode,
        @JsonProperty("banner_url")
        String bannerUrl,
        @JsonProperty("floor_payload")
        JsonNode floorPayload,
        @JsonProperty("status")
        @NotBlank(message = "专题会场状态不能为空")
        String status) {
}
