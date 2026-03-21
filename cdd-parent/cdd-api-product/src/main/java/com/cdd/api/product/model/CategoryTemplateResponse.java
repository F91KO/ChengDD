package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CategoryTemplateResponse(
        long id,
        @JsonProperty("template_name")
        String templateName,
        @JsonProperty("industry_code")
        String industryCode,
        @JsonProperty("template_version")
        String templateVersion,
        @JsonProperty("max_level")
        int maxLevel,
        String status,
        @JsonProperty("template_desc")
        String templateDesc,
        @JsonProperty("categories")
        List<CategoryTemplateNodeResponse> categories) {
}
