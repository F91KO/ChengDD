package com.cdd.api.release.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReleaseTaskRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("mini_program_id")
        @NotNull(message = "小程序ID不能为空")
        Long miniProgramId,
        @JsonProperty("template_version_id")
        @NotNull(message = "模板版本ID不能为空")
        Long templateVersionId,
        @JsonProperty("release_type")
        @NotBlank(message = "发布类型不能为空")
        String releaseType,
        @JsonProperty("trigger_source")
        @NotBlank(message = "触发来源不能为空")
        String triggerSource,
        @JsonProperty("release_snapshot_json")
        String releaseSnapshotJson) {
}
