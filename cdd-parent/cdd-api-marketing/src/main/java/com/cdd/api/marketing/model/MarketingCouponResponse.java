package com.cdd.api.marketing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record MarketingCouponResponse(
        @JsonProperty("id")
        Long id,
        @JsonProperty("merchant_id")
        Long merchantId,
        @JsonProperty("store_id")
        Long storeId,
        @JsonProperty("coupon_name")
        String couponName,
        @JsonProperty("coupon_type")
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
        String status) {
}
