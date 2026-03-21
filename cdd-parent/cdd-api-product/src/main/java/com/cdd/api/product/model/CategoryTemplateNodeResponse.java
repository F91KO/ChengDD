package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CategoryTemplateNodeResponse(
        long id,
        @JsonProperty("template_category_code")
        String templateCategoryCode,
        @JsonProperty("parent_template_category_code")
        String parentTemplateCategoryCode,
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
