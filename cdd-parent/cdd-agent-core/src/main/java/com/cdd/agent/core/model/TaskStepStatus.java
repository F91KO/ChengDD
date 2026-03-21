package com.cdd.agent.core.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 任务步骤状态机定义。
 */
public enum TaskStepStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELED,
    SKIPPED;

    private static final Map<TaskStepStatus, Set<TaskStepStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, EnumSet.of(RUNNING, CANCELED, SKIPPED),
            RUNNING, EnumSet.of(SUCCEEDED, FAILED, CANCELED),
            SUCCEEDED, EnumSet.noneOf(TaskStepStatus.class),
            FAILED, EnumSet.noneOf(TaskStepStatus.class),
            CANCELED, EnumSet.noneOf(TaskStepStatus.class),
            SKIPPED, EnumSet.noneOf(TaskStepStatus.class));

    public boolean canTransitionTo(TaskStepStatus targetStatus) {
        Objects.requireNonNull(targetStatus, "目标步骤状态不能为空");
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(targetStatus);
    }

    public void validateTransitionTo(TaskStepStatus targetStatus) {
        if (!canTransitionTo(targetStatus)) {
            throw new IllegalStateException("步骤状态不允许从 " + this + " 变更为 " + targetStatus);
        }
    }
}
