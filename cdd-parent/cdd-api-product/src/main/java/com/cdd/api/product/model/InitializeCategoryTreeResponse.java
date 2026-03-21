package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InitializeCategoryTreeResponse(
        @JsonProperty("merchant_id")
        long merchantId,
        @JsonProperty("store_id")
        long storeId,
        @JsonProperty("template_id")
        long templateId,
        @JsonProperty("initialized_category_count")
        int initializedCategoryCount,
        String message) {
}
