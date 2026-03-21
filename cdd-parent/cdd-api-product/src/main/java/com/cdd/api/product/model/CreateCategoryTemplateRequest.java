package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateCategoryTemplateRequest(
        @JsonProperty("template_name")
        @NotBlank(message = "模板名称不能为空")
        @Size(max = 128, message = "模板名称长度不能超过128")
        String templateName,
        @JsonProperty("industry_code")
        @NotBlank(message = "行业编码不能为空")
        @Size(max = 64, message = "行业编码长度不能超过64")
        String industryCode,
        @JsonProperty("template_version")
        @NotBlank(message = "模板版本不能为空")
        @Size(max = 32, message = "模板版本长度不能超过32")
        String templateVersion,
        @JsonProperty("max_level")
        @NotNull(message = "最大层级不能为空")
        @Min(value = 1, message = "最大层级最小为1")
        @Max(value = 6, message = "最大层级最大为6")
        Integer maxLevel,
        @JsonProperty("template_desc")
        @Size(max = 512, message = "模板说明长度不能超过512")
        String templateDesc,
        @JsonProperty("categories")
        @NotEmpty(message = "模板分类节点不能为空")
        @Valid
        List<CategoryTemplateNodeRequest> categories) {
}
