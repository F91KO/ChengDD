package com.cdd.agent.core.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 任务实例模型。
 */
public final class TaskInstance {

    private final String taskNo;
    private final String taskType;
    private TaskStatus status;
    private int retryCount;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private final List<TaskStepInstance> steps;

    public TaskInstance(String taskNo, TaskDefinition definition) {
        if (taskNo == null || taskNo.isBlank()) {
            throw new IllegalArgumentException("任务编号不能为空");
        }
        Objects.requireNonNull(definition, "任务定义不能为空");
        this.taskNo = taskNo;
        this.taskType = definition.taskType();
        this.status = TaskStatus.PENDING;
        List<TaskStepInstance> stepInstances = new ArrayList<>();
        for (TaskStepDefinition stepDefinition : definition.steps()) {
            stepInstances.add(new TaskStepInstance(stepDefinition));
        }
        this.steps = Collections.unmodifiableList(stepInstances);
    }

    public String taskNo() {
        return taskNo;
    }

    public String taskType() {
        return taskType;
    }

    public TaskStatus status() {
        return status;
    }

    public int retryCount() {
        return retryCount;
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

    public List<TaskStepInstance> steps() {
        return steps;
    }

    public void transitionTo(TaskStatus targetStatus, LocalDateTime changedAt) {
        Objects.requireNonNull(targetStatus, "目标任务状态不能为空");
        Objects.requireNonNull(changedAt, "任务状态变更时间不能为空");
        status.validateTransitionTo(targetStatus);
        this.status = targetStatus;
        if (targetStatus == TaskStatus.RUNNING && startedAt == null) {
            this.startedAt = changedAt;
        }
        if (targetStatus.isTerminal()) {
            this.finishedAt = changedAt;
        }
        if (targetStatus != TaskStatus.FAILED) {
            this.errorMessage = null;
        }
    }

    public void markFailed(String message, LocalDateTime failedAt) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("任务失败原因不能为空");
        }
        transitionTo(TaskStatus.FAILED, failedAt);
        this.errorMessage = message;
    }

    public void incrementRetryCount() {
        if (status != TaskStatus.FAILED) {
            throw new IllegalStateException("只有失败状态的任务才能增加重试次数");
        }
        this.retryCount++;
    }
}
