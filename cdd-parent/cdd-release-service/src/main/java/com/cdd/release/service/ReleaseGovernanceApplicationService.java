package com.cdd.release.service;

import com.cdd.api.release.model.CreateReleaseTaskRequest;
import com.cdd.api.release.model.ReleaseTaskResponse;
import com.cdd.api.release.model.ReleaseTaskResultSyncRequest;
import com.cdd.api.release.model.ReleaseTaskRollbackRequest;
import com.cdd.api.release.model.ReleaseTaskStatusUpdateRequest;
import com.cdd.api.release.model.ReleaseTaskStepResponse;
import com.cdd.api.release.model.ReleaseTaskStepUpdateRequest;
import com.cdd.common.core.error.BusinessException;
import com.cdd.release.domain.ReleaseMappingStatus;
import com.cdd.release.domain.ReleaseStepStatus;
import com.cdd.release.domain.ReleaseTaskStatus;
import com.cdd.release.domain.ResultSyncStatus;
import com.cdd.release.error.ReleaseErrorCode;
import com.cdd.release.infrastructure.persistence.ReleaseGovernanceRepository;
import com.cdd.release.infrastructure.persistence.ReleaseGovernanceRepository.StoredReleaseTask;
import com.cdd.release.infrastructure.persistence.ReleaseGovernanceRepository.StoredReleaseTaskStep;
import com.cdd.release.infrastructure.persistence.ReleaseGovernanceRepository.StoredReleaseVersionMapping;
import com.cdd.release.infrastructure.persistence.ReleaseGovernanceRepository.StoredRollbackRecord;
import com.cdd.release.infrastructure.persistence.ReleaseGovernanceRepository.StoredTemplateVersion;
import com.cdd.release.support.IdGenerator;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReleaseGovernanceApplicationService {

    private static final String RELEASE_TYPE_ROLLBACK = "rollback";
    private static final String TRIGGER_SOURCE_SYSTEM_ROLLBACK = "system_rollback";

    private final ReleaseGovernanceRepository repository;
    private final IdGenerator idGenerator;

    public ReleaseGovernanceApplicationService(ReleaseGovernanceRepository repository, IdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public ReleaseTaskResponse createReleaseTask(CreateReleaseTaskRequest request) {
        StoredTemplateVersion templateVersion = requireTemplateVersion(request.templateVersionId());
        requireMiniProgram(request.merchantId(), request.storeId(), request.miniProgramId());
        Instant now = Instant.now();
        long taskId = idGenerator.nextId();
        String taskNo = "rls_" + taskId;
        StoredReleaseTask task = new StoredReleaseTask(
                taskId,
                taskNo,
                request.merchantId(),
                request.storeId(),
                request.miniProgramId(),
                request.templateVersionId(),
                normalize(request.releaseType()),
                ReleaseTaskStatus.PENDING.value(),
                normalize(request.triggerSource()),
                trimToNull(request.releaseSnapshotJson()),
                "task_created",
                ResultSyncStatus.PENDING.value(),
                null,
                null,
                null,
                now,
                null);
        repository.createReleaseTask(task);
        StoredReleaseTaskStep initStep = new StoredReleaseTaskStep(
                idGenerator.nextId(),
                taskId,
                "task_created",
                "创建发布任务",
                1,
                ReleaseStepStatus.SUCCESS.value(),
                "发布任务已创建",
                null,
                0,
                now,
                now);
        repository.createReleaseTaskStep(initStep);
        appendLog(task.id(), "INFO", "task_create",
                "创建发布任务成功，taskNo=" + task.taskNo() + "，templateVersion=" + templateVersion.templateVersion());
        return toResponse(task, List.of(initStep));
    }

    public ReleaseTaskResponse getReleaseTask(String taskNo) {
        StoredReleaseTask task = requireReleaseTask(taskNo);
        return toResponse(task, repository.listReleaseTaskSteps(task.id()));
    }

    @Transactional
    public ReleaseTaskResponse updateReleaseTaskStatus(String taskNo, ReleaseTaskStatusUpdateRequest request) {
        StoredReleaseTask task = requireReleaseTask(taskNo);
        ReleaseTaskStatus currentStatus = parseTaskStatus(task.releaseStatus());
        ReleaseTaskStatus targetStatus = ReleaseTaskStatus.from(request.targetStatus());
        if (targetStatus == null) {
            throw new BusinessException(ReleaseErrorCode.RELEASE_STATUS_INVALID);
        }
        if (!currentStatus.canTransitTo(targetStatus)) {
            throw new BusinessException(ReleaseErrorCode.RELEASE_STATUS_TRANSITION_INVALID,
                    "发布状态不允许从" + currentStatus.value() + "变更为" + targetStatus.value());
        }
        if (currentStatus == targetStatus
                && !StringUtils.hasText(request.currentStepCode())
                && !StringUtils.hasText(request.errorCode())
                && !StringUtils.hasText(request.errorMessage())) {
            return toResponse(task, repository.listReleaseTaskSteps(task.id()));
        }
        Instant now = Instant.now();
        String currentStepCode = StringUtils.hasText(request.currentStepCode())
                ? request.currentStepCode().trim()
                : task.currentStepCode();
        String errorCode = StringUtils.hasText(request.errorCode()) ? request.errorCode().trim() : null;
        String errorMessage = StringUtils.hasText(request.errorMessage()) ? request.errorMessage().trim() : null;
        StoredReleaseTask updated = new StoredReleaseTask(
                task.id(),
                task.taskNo(),
                task.merchantId(),
                task.storeId(),
                task.miniProgramId(),
                task.templateVersionId(),
                task.releaseType(),
                targetStatus.value(),
                task.triggerSource(),
                task.releaseSnapshotJson(),
                currentStepCode,
                task.resultSyncStatus(),
                task.rollbackTaskNo(),
                errorCode,
                errorMessage,
                task.startedAt(),
                targetStatus.terminal() ? now : null);
        repository.updateReleaseTask(updated);
        appendLog(updated.id(), targetStatus == ReleaseTaskStatus.FAILED ? "WARN" : "INFO", "status_update",
                "发布状态变更为" + targetStatus.value());
        return toResponse(updated, repository.listReleaseTaskSteps(updated.id()));
    }

    @Transactional
    public ReleaseTaskResponse updateReleaseTaskStep(String taskNo, ReleaseTaskStepUpdateRequest request) {
        StoredReleaseTask task = requireReleaseTask(taskNo);
        ReleaseStepStatus stepStatus = ReleaseStepStatus.from(request.stepStatus());
        if (stepStatus == null) {
            throw new BusinessException(ReleaseErrorCode.RELEASE_STEP_STATUS_INVALID);
        }
        String stepCode = resolveStepCode(request.stepCode(), request.stepName(), request.stepOrder());
        Instant now = Instant.now();
        Optional<StoredReleaseTaskStep> existingStep = repository.findReleaseTaskStep(task.id(), stepCode);
        StoredReleaseTaskStep step = existingStep
                .map(existing -> {
                    int retryCount = existing.retryCount() + (stepStatus == ReleaseStepStatus.FAILED ? 1 : 0);
                    Instant startedAt = existing.startedAt() == null ? now : existing.startedAt();
                    Instant finishedAt = stepStatus.done() ? now : null;
                    return new StoredReleaseTaskStep(
                            existing.id(),
                            existing.taskId(),
                            existing.stepCode(),
                            request.stepName().trim(),
                            request.stepOrder(),
                            stepStatus.value(),
                            trimToNull(request.resultMessage()),
                            trimToNull(request.errorCode()),
                            retryCount,
                            startedAt,
                            finishedAt);
                })
                .orElseGet(() -> {
                    int retryCount = stepStatus == ReleaseStepStatus.FAILED ? 1 : 0;
                    Instant startedAt = stepStatus == ReleaseStepStatus.PENDING ? null : now;
                    Instant finishedAt = stepStatus.done() ? now : null;
                    return new StoredReleaseTaskStep(
                            idGenerator.nextId(),
                            task.id(),
                            stepCode,
                            request.stepName().trim(),
                            request.stepOrder(),
                            stepStatus.value(),
                            trimToNull(request.resultMessage()),
                            trimToNull(request.errorCode()),
                            retryCount,
                            startedAt,
                            finishedAt);
                });
        if (existingStep.isPresent()) {
            repository.updateReleaseTaskStep(step);
        } else {
            repository.createReleaseTaskStep(step);
        }
        ReleaseTaskStatus currentTaskStatus = parseTaskStatus(task.releaseStatus());
        ReleaseTaskStatus nextTaskStatus = currentTaskStatus;
        String lastErrorCode = task.lastErrorCode();
        String lastErrorMessage = task.lastErrorMessage();
        Instant finishedAt = task.finishedAt();
        if (stepStatus == ReleaseStepStatus.FAILED) {
            nextTaskStatus = ReleaseTaskStatus.FAILED;
            lastErrorCode = StringUtils.hasText(request.errorCode()) ? request.errorCode().trim() : "RELEASE_STEP_FAILED";
            lastErrorMessage = StringUtils.hasText(request.resultMessage()) ? request.resultMessage().trim() : "发布步骤执行失败";
            finishedAt = now;
            appendLog(task.id(), "WARN", "step_update", "步骤执行失败，stepCode=" + stepCode);
        } else if (stepStatus == ReleaseStepStatus.RUNNING || stepStatus == ReleaseStepStatus.SUCCESS) {
            if (currentTaskStatus == ReleaseTaskStatus.PENDING) {
                nextTaskStatus = ReleaseTaskStatus.RUNNING;
            }
            if (stepStatus == ReleaseStepStatus.SUCCESS) {
                if ("release_done".equals(stepCode)) {
                    nextTaskStatus = ReleaseTaskStatus.SUCCESS;
                    finishedAt = now;
                    lastErrorCode = null;
                    lastErrorMessage = null;
                } else if ("rollback_done".equals(stepCode)) {
                    nextTaskStatus = ReleaseTaskStatus.ROLLED_BACK;
                    finishedAt = now;
                    lastErrorCode = null;
                    lastErrorMessage = null;
                }
            }
            appendLog(task.id(), "INFO", "step_update", "步骤状态更新，stepCode=" + stepCode + "，status=" + stepStatus.value());
        }
        StoredReleaseTask updatedTask = new StoredReleaseTask(
                task.id(),
                task.taskNo(),
                task.merchantId(),
                task.storeId(),
                task.miniProgramId(),
                task.templateVersionId(),
                task.releaseType(),
                nextTaskStatus.value(),
                task.triggerSource(),
                task.releaseSnapshotJson(),
                stepCode,
                task.resultSyncStatus(),
                task.rollbackTaskNo(),
                lastErrorCode,
                lastErrorMessage,
                task.startedAt(),
                finishedAt);
        repository.updateReleaseTask(updatedTask);
        return toResponse(updatedTask, repository.listReleaseTaskSteps(task.id()));
    }

    @Transactional
    public ReleaseTaskResponse syncReleaseResult(String taskNo, ReleaseTaskResultSyncRequest request) {
        StoredReleaseTask task = requireReleaseTask(taskNo);
        ReleaseTaskStatus status = parseTaskStatus(task.releaseStatus());
        if (status != ReleaseTaskStatus.SUCCESS) {
            throw new BusinessException(ReleaseErrorCode.RELEASE_RESULT_SYNC_PRECONDITION_FAILED);
        }
        if (ResultSyncStatus.SYNCED.value().equals(task.resultSyncStatus())) {
            return toResponse(task, repository.listReleaseTaskSteps(task.id()));
        }
        ReleaseMappingStatus mappingStatus = ReleaseMappingStatus.from(request == null ? null : request.mappingStatus());
        if (mappingStatus == null || mappingStatus != ReleaseMappingStatus.ACTIVE) {
            throw new BusinessException(ReleaseErrorCode.RELEASE_MAPPING_STATUS_INVALID,
                    "当前仅支持active映射状态回写");
        }
        StoredTemplateVersion templateVersion = requireTemplateVersion(task.templateVersionId());
        Instant now = Instant.now();
        try {
            repository.deactivateVersionMappings(task.miniProgramId(), now);
            Optional<StoredReleaseVersionMapping> existingMapping = repository.findVersionMapping(
                    task.miniProgramId(), task.templateVersionId());
            StoredReleaseVersionMapping mapping = existingMapping
                    .map(existing -> new StoredReleaseVersionMapping(
                            existing.id(),
                            task.merchantId(),
                            task.storeId(),
                            task.miniProgramId(),
                            task.templateVersionId(),
                            templateVersion.templateCode(),
                            templateVersion.templateVersion(),
                            ReleaseMappingStatus.ACTIVE.value(),
                            task.id(),
                            now,
                            null))
                    .orElseGet(() -> new StoredReleaseVersionMapping(
                            idGenerator.nextId(),
                            task.merchantId(),
                            task.storeId(),
                            task.miniProgramId(),
                            task.templateVersionId(),
                            templateVersion.templateCode(),
                            templateVersion.templateVersion(),
                            ReleaseMappingStatus.ACTIVE.value(),
                            task.id(),
                            now,
                            null));
            if (existingMapping.isPresent()) {
                repository.updateVersionMapping(mapping);
            } else {
                repository.createVersionMapping(mapping);
            }
            repository.updateMiniProgramTemplateVersion(task.miniProgramId(), templateVersion.templateVersion());
            StoredReleaseTask syncedTask = new StoredReleaseTask(
                    task.id(),
                    task.taskNo(),
                    task.merchantId(),
                    task.storeId(),
                    task.miniProgramId(),
                    task.templateVersionId(),
                    task.releaseType(),
                    task.releaseStatus(),
                    task.triggerSource(),
                    task.releaseSnapshotJson(),
                    task.currentStepCode(),
                    ResultSyncStatus.SYNCED.value(),
                    task.rollbackTaskNo(),
                    null,
                    null,
                    task.startedAt(),
                    task.finishedAt());
            repository.updateReleaseTask(syncedTask);
            appendLog(task.id(), "INFO", "result_sync", "发布结果回写成功，模板版本=" + templateVersion.templateVersion());
            return toResponse(syncedTask, repository.listReleaseTaskSteps(task.id()));
        } catch (Exception ex) {
            StoredReleaseTask failedSyncTask = new StoredReleaseTask(
                    task.id(),
                    task.taskNo(),
                    task.merchantId(),
                    task.storeId(),
                    task.miniProgramId(),
                    task.templateVersionId(),
                    task.releaseType(),
                    task.releaseStatus(),
                    task.triggerSource(),
                    task.releaseSnapshotJson(),
                    task.currentStepCode(),
                    ResultSyncStatus.FAILED.value(),
                    task.rollbackTaskNo(),
                    "RELEASE_RESULT_SYNC_FAILED",
                    "发布结果回写失败",
                    task.startedAt(),
                    task.finishedAt());
            repository.updateReleaseTask(failedSyncTask);
            appendLog(task.id(), "ERROR", "result_sync", "发布结果回写失败");
            throw new BusinessException(ReleaseErrorCode.RELEASE_RESULT_SYNC_FAILED);
        }
    }

    @Transactional
    public ReleaseTaskResponse createRollbackTask(String taskNo, ReleaseTaskRollbackRequest request) {
        StoredReleaseTask sourceTask = requireReleaseTask(taskNo);
        if (StringUtils.hasText(sourceTask.rollbackTaskNo())) {
            return toResponse(sourceTask, repository.listReleaseTaskSteps(sourceTask.id()));
        }
        ReleaseTaskStatus currentStatus = parseTaskStatus(sourceTask.releaseStatus());
        if (currentStatus != ReleaseTaskStatus.SUCCESS && currentStatus != ReleaseTaskStatus.FAILED) {
            throw new BusinessException(ReleaseErrorCode.RELEASE_ROLLBACK_NOT_ALLOWED);
        }
        Instant now = Instant.now();
        long rollbackTaskId = idGenerator.nextId();
        String rollbackTaskNo = "rls_rb_" + rollbackTaskId;
        String snapshot = """
                {"source_task_no":"%s","rollback_target_version":"%s","rollback_reason":"%s"}
                """.formatted(
                escapeJson(sourceTask.taskNo()),
                escapeJson(request.rollbackTargetVersion()),
                escapeJson(request.rollbackReason()));
        StoredReleaseTask rollbackTask = new StoredReleaseTask(
                rollbackTaskId,
                rollbackTaskNo,
                sourceTask.merchantId(),
                sourceTask.storeId(),
                sourceTask.miniProgramId(),
                sourceTask.templateVersionId(),
                RELEASE_TYPE_ROLLBACK,
                ReleaseTaskStatus.ROLLING_BACK.value(),
                TRIGGER_SOURCE_SYSTEM_ROLLBACK,
                snapshot.trim(),
                "rollback_init",
                ResultSyncStatus.PENDING.value(),
                null,
                null,
                null,
                now,
                null);
        repository.createReleaseTask(rollbackTask);
        repository.createReleaseTaskStep(new StoredReleaseTaskStep(
                idGenerator.nextId(),
                rollbackTask.id(),
                "rollback_init",
                "创建回滚任务",
                1,
                ReleaseStepStatus.SUCCESS.value(),
                "回滚任务骨架已创建",
                null,
                0,
                now,
                now));
        repository.createRollbackRecord(new StoredRollbackRecord(
                idGenerator.nextId(),
                sourceTask.id(),
                request.rollbackTargetVersion().trim(),
                request.rollbackReason().trim(),
                "pending",
                null));
        StoredReleaseTask updatedSourceTask = new StoredReleaseTask(
                sourceTask.id(),
                sourceTask.taskNo(),
                sourceTask.merchantId(),
                sourceTask.storeId(),
                sourceTask.miniProgramId(),
                sourceTask.templateVersionId(),
                sourceTask.releaseType(),
                ReleaseTaskStatus.ROLLING_BACK.value(),
                sourceTask.triggerSource(),
                sourceTask.releaseSnapshotJson(),
                "rollback_init",
                sourceTask.resultSyncStatus(),
                rollbackTaskNo,
                sourceTask.lastErrorCode(),
                sourceTask.lastErrorMessage(),
                sourceTask.startedAt(),
                null);
        repository.updateReleaseTask(updatedSourceTask);
        appendLog(sourceTask.id(), "WARN", "rollback_create", "创建回滚任务成功，rollbackTaskNo=" + rollbackTaskNo);
        appendLog(rollbackTask.id(), "INFO", "rollback_create", "回滚任务骨架已创建");
        return toResponse(updatedSourceTask, repository.listReleaseTaskSteps(sourceTask.id()));
    }

    private StoredReleaseTask requireReleaseTask(String taskNo) {
        return repository.findReleaseTaskByTaskNo(taskNo)
                .orElseThrow(() -> new BusinessException(ReleaseErrorCode.RELEASE_TASK_NOT_FOUND));
    }

    private StoredTemplateVersion requireTemplateVersion(long templateVersionId) {
        return repository.findTemplateVersionById(templateVersionId)
                .orElseThrow(() -> new BusinessException(ReleaseErrorCode.TEMPLATE_VERSION_NOT_FOUND));
    }

    private void requireMiniProgram(long merchantId, long storeId, long miniProgramId) {
        if (!repository.miniProgramExists(merchantId, storeId, miniProgramId)) {
            throw new BusinessException(ReleaseErrorCode.MINI_PROGRAM_NOT_FOUND);
        }
    }

    private ReleaseTaskStatus parseTaskStatus(String rawStatus) {
        ReleaseTaskStatus status = ReleaseTaskStatus.from(rawStatus);
        if (status == null) {
            throw new BusinessException(ReleaseErrorCode.RELEASE_STATUS_INVALID);
        }
        return status;
    }

    private ReleaseTaskResponse toResponse(StoredReleaseTask task, List<StoredReleaseTaskStep> steps) {
        return new ReleaseTaskResponse(
                task.taskNo(),
                task.merchantId(),
                task.storeId(),
                task.miniProgramId(),
                task.templateVersionId(),
                task.releaseType(),
                task.releaseStatus(),
                task.triggerSource(),
                task.currentStepCode(),
                task.resultSyncStatus(),
                task.rollbackTaskNo(),
                task.lastErrorCode(),
                task.lastErrorMessage(),
                task.startedAt(),
                task.finishedAt(),
                steps.stream()
                        .map(step -> new ReleaseTaskStepResponse(
                                step.stepCode(),
                                step.stepName(),
                                step.stepOrder(),
                                step.stepStatus(),
                                step.resultMessage(),
                                step.errorCode(),
                                step.retryCount(),
                                step.startedAt(),
                                step.finishedAt()))
                        .toList());
    }

    private void appendLog(long taskId, String level, String stage, String content) {
        repository.appendReleaseLog(idGenerator.nextId(), taskId, level, stage, content, Instant.now());
    }

    private static String resolveStepCode(String stepCode, String stepName, int stepOrder) {
        if (StringUtils.hasText(stepCode)) {
            return trimToLength(stepCode.trim().toLowerCase(Locale.ROOT), 64);
        }
        String normalized = normalizeStepSegment(stepName);
        return trimToLength(normalized + "_" + stepOrder, 64);
    }

    private static String normalizeStepSegment(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        String sanitized = value.replaceAll("[^a-z0-9]+", "_");
        sanitized = sanitized.replaceAll("^_+|_+$", "");
        return sanitized.isBlank() ? "step" : sanitized;
    }

    private static String trimToLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.trim();
    }

    private static String escapeJson(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
