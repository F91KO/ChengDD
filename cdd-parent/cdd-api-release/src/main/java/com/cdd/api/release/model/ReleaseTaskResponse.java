package com.cdd.api.release.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record ReleaseTaskResponse(
        @JsonProperty("task_no")
        String taskNo,
        @JsonProperty("merchant_id")
        long merchantId,
        @JsonProperty("store_id")
        long storeId,
        @JsonProperty("mini_program_id")
        long miniProgramId,
        @JsonProperty("template_version_id")
        long templateVersionId,
        @JsonProperty("release_type")
        String releaseType,
        @JsonProperty("release_status")
        String releaseStatus,
        @JsonProperty("trigger_source")
        String triggerSource,
        @JsonProperty("current_step_code")
        String currentStepCode,
        @JsonProperty("result_sync_status")
        String resultSyncStatus,
        @JsonProperty("rollback_task_no")
        String rollbackTaskNo,
        @JsonProperty("last_error_code")
        String lastErrorCode,
        @JsonProperty("last_error_message")
        String lastErrorMessage,
        @JsonProperty("started_at")
        Instant startedAt,
        @JsonProperty("finished_at")
        Instant finishedAt,
        @JsonProperty("steps")
        List<ReleaseTaskStepResponse> steps) {
}
