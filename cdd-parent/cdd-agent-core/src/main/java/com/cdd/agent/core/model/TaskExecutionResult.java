package com.cdd.agent.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * 任务执行结果模型。
 */
public record TaskExecutionResult(
        TaskStatus finalStatus,
        String summary,
        Map<String, Object> output) {

    public TaskExecutionResult {
        Objects.requireNonNull(finalStatus, "任务最终状态不能为空");
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("任务执行结果摘要不能为空");
        }
        output = output == null ? Map.of() : Map.copyOf(output);
    }

    public static TaskExecutionResult succeeded(Map<String, Object> output) {
        return new TaskExecutionResult(TaskStatus.SUCCEEDED, "执行成功", output);
    }

    public static TaskExecutionResult failed(String summary) {
        return new TaskExecutionResult(TaskStatus.FAILED, summary, Map.of());
    }
}
