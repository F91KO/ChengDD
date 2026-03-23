package com.cdd.api.decoration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DecorationHomeModuleRequest(
        @JsonProperty("module_id")
        Long moduleId,
        @JsonProperty("module_type")
        @NotBlank(message = "模块类型不能为空")
        String moduleType,
        @JsonProperty("module_name")
        @NotBlank(message = "模块名称不能为空")
        String moduleName,
        @JsonProperty("sort_order")
        Integer sortOrder,
        @JsonProperty("is_enabled")
        @NotNull(message = "模块启用状态不能为空")
        Boolean enabled,
        @JsonProperty("style_mode")
        String styleMode,
        @JsonProperty("data_source_type")
        String dataSourceType,
        @JsonProperty("data_source_id")
        Long dataSourceId,
        @JsonProperty("jump_target")
        JsonNode jumpTarget,
        @JsonProperty("config_payload")
        JsonNode configPayload) {
}
