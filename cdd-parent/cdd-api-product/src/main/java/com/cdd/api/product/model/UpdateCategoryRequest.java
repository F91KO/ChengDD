package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("category_name")
        @Size(max = 128, message = "分类名称长度不能超过128")
        String categoryName,
        @JsonProperty("sort_order")
        Integer sortOrder,
        @JsonProperty("is_enabled")
        Boolean enabled,
        @JsonProperty("is_visible")
        Boolean visible) {
}
