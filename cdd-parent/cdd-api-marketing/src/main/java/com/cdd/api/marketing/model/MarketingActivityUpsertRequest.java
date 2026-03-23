package com.cdd.api.marketing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MarketingActivityUpsertRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("activity_name")
        @NotBlank(message = "活动名称不能为空")
        String activityName,
        @JsonProperty("activity_type")
        @NotBlank(message = "活动类型不能为空")
        String activityType,
        @JsonProperty("activity_status")
        @NotBlank(message = "活动状态不能为空")
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
        @NotEmpty(message = "活动商品不能为空")
        List<@Valid ActivityProductRequest> products) {
}
