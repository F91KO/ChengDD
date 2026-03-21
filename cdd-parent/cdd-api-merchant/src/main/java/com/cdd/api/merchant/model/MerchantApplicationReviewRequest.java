package com.cdd.api.merchant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MerchantApplicationReviewRequest(
        @NotBlank(message = "审核动作不能为空")
        @Size(max = 16, message = "审核动作长度不能超过16")
        String decision,
        @JsonProperty("reject_reason")
        @Size(max = 512, message = "驳回原因长度不能超过512")
        String rejectReason) {
}
