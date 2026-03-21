package com.cdd.api.release.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ReleaseTaskRollbackRequest(
        @JsonProperty("rollback_target_version")
        @NotBlank(message = "回滚目标版本不能为空")
        String rollbackTargetVersion,
        @JsonProperty("rollback_reason")
        @NotBlank(message = "回滚原因不能为空")
        String rollbackReason) {
}
