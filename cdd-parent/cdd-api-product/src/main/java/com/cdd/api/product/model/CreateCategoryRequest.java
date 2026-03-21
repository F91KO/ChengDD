package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCategoryRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("parent_id")
        Long parentId,
        @JsonProperty("category_name")
        @NotBlank(message = "分类名称不能为空")
        String categoryName,
        @JsonProperty("sort_order")
        Integer sortOrder,
        @JsonProperty("is_enabled")
        Boolean enabled,
        @JsonProperty("is_visible")
        Boolean visible) {
}
