package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CategoryResponse(
        long id,
        @JsonProperty("merchant_id")
        long merchantId,
        @JsonProperty("store_id")
        long storeId,
        @JsonProperty("template_id")
        Long templateId,
        @JsonProperty("parent_id")
        long parentId,
        @JsonProperty("category_name")
        String categoryName,
        @JsonProperty("category_level")
        int categoryLevel,
        @JsonProperty("sort_order")
        int sortOrder,
        @JsonProperty("is_enabled")
        boolean enabled,
        @JsonProperty("is_visible")
        boolean visible) {
}
