package com.cdd.release.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReleaseGovernanceRepository {

    void createReleaseTask(StoredReleaseTask task);

    Optional<StoredReleaseTask> findReleaseTaskByTaskNo(String taskNo);

    void updateReleaseTask(StoredReleaseTask task);

    void createReleaseTaskStep(StoredReleaseTaskStep step);

    Optional<StoredReleaseTaskStep> findReleaseTaskStep(long taskId, String stepCode);

    void updateReleaseTaskStep(StoredReleaseTaskStep step);

    List<StoredReleaseTaskStep> listReleaseTaskSteps(long taskId);

    void appendReleaseLog(long logId, long taskId, String logLevel, String logStage, String logContent, Instant createdAt);

    Optional<StoredTemplateVersion> findTemplateVersionById(long templateVersionId);

    boolean miniProgramExists(long merchantId, long storeId, long miniProgramId);

    void updateMiniProgramTemplateVersion(long miniProgramId, String currentTemplateVersion);

    void deactivateVersionMappings(long miniProgramId, Instant deactivatedAt);

    Optional<StoredReleaseVersionMapping> findVersionMapping(long miniProgramId, long templateVersionId);

    void createVersionMapping(StoredReleaseVersionMapping mapping);

    void updateVersionMapping(StoredReleaseVersionMapping mapping);

    void createRollbackRecord(StoredRollbackRecord rollbackRecord);

    record StoredTemplateVersion(
            long id,
            String templateCode,
            String templateVersion) {
    }

    record StoredReleaseTask(
            long id,
            String taskNo,
            long merchantId,
            long storeId,
            long miniProgramId,
            long templateVersionId,
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
            Instant finishedAt) {
    }

    record StoredReleaseTaskStep(
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

    record StoredReleaseVersionMapping(
            long id,
            long merchantId,
            long storeId,
            long miniProgramId,
            long templateVersionId,
            String templateCode,
            String templateVersion,
            String mappingStatus,
            long sourceTaskId,
            Instant activatedAt,
            Instant deactivatedAt) {
    }

    record StoredRollbackRecord(
            long id,
            long taskId,
            String rollbackTargetVersion,
            String rollbackReason,
            String rollbackStatus,
            Instant rolledBackAt) {
    }
}
