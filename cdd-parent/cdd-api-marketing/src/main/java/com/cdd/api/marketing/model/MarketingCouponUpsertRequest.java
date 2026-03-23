package com.cdd.api.marketing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MarketingCouponUpsertRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("coupon_name")
        @NotBlank(message = "优惠券名称不能为空")
        String couponName,
        @JsonProperty("coupon_type")
        @NotBlank(message = "优惠券类型不能为空")
        String couponType,
        @JsonProperty("threshold_amount")
        BigDecimal thresholdAmount,
        @JsonProperty("discount_amount")
        BigDecimal discountAmount,
        @JsonProperty("discount_rate")
        BigDecimal discountRate,
        @JsonProperty("issue_start_at")
        String issueStartAt,
        @JsonProperty("issue_end_at")
        String issueEndAt,
        @JsonProperty("status")
        @NotBlank(message = "优惠券状态不能为空")
        String status) {
}
