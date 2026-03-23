package com.cdd.api.marketing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MarketingRecommendRuleUpsertRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("rule_name")
        @NotBlank(message = "推荐规则名称不能为空")
        String ruleName,
        @JsonProperty("scene_code")
        @NotBlank(message = "场景编码不能为空")
        String sceneCode,
        @JsonProperty("rule_payload")
        JsonNode rulePayload,
        @JsonProperty("status")
        @NotBlank(message = "推荐规则状态不能为空")
        String status) {
}
