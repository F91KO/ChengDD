package com.cdd.api.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ConfigPublishRollbackRequest(
        @JsonProperty("operator_name")
        @NotBlank(message = "操作人不能为空")
        String operatorName,
        @JsonProperty("rollback_reason")
        @NotBlank(message = "回滚原因不能为空")
        String rollbackReason) {
}
