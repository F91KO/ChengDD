package com.cdd.api.merchant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record MerchantApplicationResponse(
        @JsonProperty("application_id")
        long applicationId,
        @JsonProperty("merchant_name")
        String merchantName,
        @JsonProperty("merchant_type")
        String merchantType,
        @JsonProperty("contact_name")
        String contactName,
        @JsonProperty("contact_mobile")
        String contactMobile,
        String status,
        @JsonProperty("reject_reason")
        String rejectReason,
        @JsonProperty("submitted_at")
        Instant submittedAt) {
}
