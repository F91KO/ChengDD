package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderAfterSaleReviewRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("operator_id")
        @NotNull(message = "操作人ID不能为空")
        Long operatorId,
        @JsonProperty("review_action")
        @NotBlank(message = "审核动作不能为空")
        String reviewAction,
        @JsonProperty("merchant_result")
        String merchantResult) {
}
