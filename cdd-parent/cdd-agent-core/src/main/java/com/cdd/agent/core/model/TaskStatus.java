package com.cdd.agent.core.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 任务状态机定义。
 */
public enum TaskStatus {
    PENDING,
    READY,
    RUNNING,
    WAITING_APPROVAL,
    SUCCEEDED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    CANCELED;

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, EnumSet.of(READY, CANCELED),
            READY, EnumSet.of(RUNNING, CANCELED),
            RUNNING, EnumSet.of(SUCCEEDED, FAILED, WAITING_APPROVAL, COMPENSATING),
            WAITING_APPROVAL, EnumSet.of(READY, CANCELED),
            SUCCEEDED, EnumSet.noneOf(TaskStatus.class),
            FAILED, EnumSet.of(READY, COMPENSATING),
            COMPENSATING, EnumSet.of(COMPENSATED, FAILED),
            COMPENSATED, EnumSet.noneOf(TaskStatus.class),
            CANCELED, EnumSet.noneOf(TaskStatus.class));

    public boolean canTransitionTo(TaskStatus targetStatus) {
        Objects.requireNonNull(targetStatus, "目标状态不能为空");
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(targetStatus);
    }

    public void validateTransitionTo(TaskStatus targetStatus) {
        if (!canTransitionTo(targetStatus)) {
            throw new IllegalStateException("任务状态不允许从 " + this + " 变更为 " + targetStatus);
        }
    }

    public boolean isTerminal() {
        return this == SUCCEEDED || this == COMPENSATED || this == CANCELED;
    }
}
