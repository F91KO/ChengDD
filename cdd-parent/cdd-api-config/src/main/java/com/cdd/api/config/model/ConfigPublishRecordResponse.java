package com.cdd.api.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ConfigPublishRecordResponse(
        @JsonProperty("task_no")
        String taskNo,
        @JsonProperty("merchant_id")
        String merchantId,
        @JsonProperty("store_id")
        String storeId,
        @JsonProperty("release_type")
        String releaseType,
        @JsonProperty("release_status")
        String releaseStatus,
        @JsonProperty("trigger_source")
        String triggerSource,
        @JsonProperty("operator_name")
        String operatorName,
        @JsonProperty("publish_note")
        String publishNote,
        @JsonProperty("rollback_reason")
        String rollbackReason,
        @JsonProperty("rollback_target_task_no")
        String rollbackTargetTaskNo,
        @JsonProperty("rollback_task_no")
        String rollbackTaskNo,
        @JsonProperty("started_at")
        String startedAt,
        @JsonProperty("finished_at")
        String finishedAt,
        @JsonProperty("created_at")
        String createdAt,
        @JsonProperty("config_count")
        Integer configCount,
        @JsonProperty("feature_switch_count")
        Integer featureSwitchCount,
        @JsonProperty("snapshot")
        ConfigPublishSnapshotResponse snapshot,
        @JsonProperty("steps")
        List<ConfigPublishStepResponse> steps) {
}
