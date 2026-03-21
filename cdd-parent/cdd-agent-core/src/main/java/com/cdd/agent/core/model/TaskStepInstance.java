package com.cdd.agent.core.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 任务步骤实例模型。
 */
public final class TaskStepInstance {

    private final TaskStepDefinition definition;
    private TaskStepStatus status;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public TaskStepInstance(TaskStepDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "步骤定义不能为空");
        this.status = TaskStepStatus.PENDING;
    }

    public TaskStepDefinition definition() {
        return definition;
    }

    public TaskStepStatus status() {
        return status;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public LocalDateTime startedAt() {
        return startedAt;
    }

    public LocalDateTime finishedAt() {
        return finishedAt;
    }

    public void transitionTo(TaskStepStatus targetStatus, LocalDateTime changedAt) {
        Objects.requireNonNull(targetStatus, "目标步骤状态不能为空");
        Objects.requireNonNull(changedAt, "步骤状态变更时间不能为空");
        status.validateTransitionTo(targetStatus);
        this.status = targetStatus;
        if (targetStatus == TaskStepStatus.RUNNING && startedAt == null) {
            this.startedAt = changedAt;
        }
        if (targetStatus == TaskStepStatus.SUCCEEDED
                || targetStatus == TaskStepStatus.FAILED
                || targetStatus == TaskStepStatus.CANCELED
                || targetStatus == TaskStepStatus.SKIPPED) {
            this.finishedAt = changedAt;
        }
        if (targetStatus != TaskStepStatus.FAILED) {
            this.errorMessage = null;
        }
    }

    public void markFailed(String message, LocalDateTime failedAt) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("步骤失败原因不能为空");
        }
        transitionTo(TaskStepStatus.FAILED, failedAt);
        this.errorMessage = message;
    }
}
