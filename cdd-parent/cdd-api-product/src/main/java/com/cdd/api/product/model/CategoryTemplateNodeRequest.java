package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryTemplateNodeRequest(
        @JsonProperty("template_category_code")
        @NotBlank(message = "模板分类编码不能为空")
        @Size(max = 64, message = "模板分类编码长度不能超过64")
        String templateCategoryCode,
        @JsonProperty("parent_template_category_code")
        @Size(max = 64, message = "父级模板分类编码长度不能超过64")
        String parentTemplateCategoryCode,
        @JsonProperty("category_name")
        @NotBlank(message = "模板分类名称不能为空")
        @Size(max = 128, message = "模板分类名称长度不能超过128")
        String categoryName,
        @JsonProperty("sort_order")
        Integer sortOrder,
        @JsonProperty("is_enabled")
        Boolean enabled,
        @JsonProperty("is_visible")
        Boolean visible) {
}
