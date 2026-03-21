package com.cdd.api.merchant.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MerchantOnboardingTaskResponse(
        @JsonProperty("task_no")
        String taskNo,
        @JsonProperty("task_status")
        String taskStatus,
        @JsonProperty("step_code")
        String stepCode,
        @JsonProperty("merchant_id")
        Long merchantId,
        @JsonProperty("store_id")
        Long storeId,
        @JsonProperty("mini_program_id")
        Long miniProgramId,
        @JsonProperty("category_template_id")
        Long categoryTemplateId,
        @JsonProperty("initialized_category_count")
        Integer initializedCategoryCount,
        String message) {
}
