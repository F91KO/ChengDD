package com.cdd.api.decoration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record DecorationHomeModuleResponse(
        @JsonProperty("module_id")
        Long moduleId,
        @JsonProperty("module_type")
        String moduleType,
        @JsonProperty("module_name")
        String moduleName,
        @JsonProperty("sort_order")
        Integer sortOrder,
        @JsonProperty("is_enabled")
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
