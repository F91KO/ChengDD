package com.cdd.config.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConfigPublishRepository {

    void createTask(PublishTaskRecord task);

    Optional<PublishTaskRecord> findTaskByTaskNo(String taskNo);

    List<PublishTaskRecord> listTasks(Long merchantId, Long storeId);

    void updateRollbackTaskNo(long taskId, String rollbackTaskNo);

    void createStep(PublishStepRecord step);

    List<PublishStepRecord> listSteps(long taskId);

    void createRollbackRecord(PublishRollbackRecord rollbackRecord);

    Optional<PublishRollbackRecord> findRollbackRecord(long taskId);

    record PublishTaskRecord(
            long id,
            String taskNo,
            long merchantId,
            long storeId,
            String releaseType,
            String releaseStatus,
            String triggerSource,
            String releaseSnapshotJson,
            String currentStepCode,
            String resultSyncStatus,
            String rollbackTaskNo,
            String lastErrorCode,
            String lastErrorMessage,
            Instant startedAt,
            Instant finishedAt,
            Instant createdAt) {
    }

    record PublishStepRecord(
            long id,
            long taskId,
            String stepCode,
            String stepName,
            int stepOrder,
            String stepStatus,
            String resultMessage,
            String errorCode,
            int retryCount,
            Instant startedAt,
            Instant finishedAt) {
    }

    record PublishRollbackRecord(
            long id,
            long taskId,
            String rollbackTargetTaskNo,
            String rollbackReason,
            String rollbackStatus,
            Instant rolledBackAt) {
    }
}
