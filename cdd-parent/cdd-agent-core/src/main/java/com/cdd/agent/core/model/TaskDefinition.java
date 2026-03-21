package com.cdd.agent.core.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 任务定义模型。
 */
public record TaskDefinition(
        String taskType,
        String taskName,
        String bizType,
        int maxRetryCount,
        int timeoutSeconds,
        boolean requiresApproval,
        boolean compensationEnabled,
        List<TaskStepDefinition> steps) {

    public TaskDefinition {
        if (isBlank(taskType)) {
            throw new IllegalArgumentException("任务类型不能为空");
        }
        if (isBlank(taskName)) {
            throw new IllegalArgumentException("任务名称不能为空");
        }
        if (isBlank(bizType)) {
            throw new IllegalArgumentException("业务类型不能为空");
        }
        if (maxRetryCount < 0) {
            throw new IllegalArgumentException("最大重试次数不能小于 0");
        }
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("超时时间必须大于 0");
        }
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("任务步骤不能为空");
        }
        steps = List.copyOf(steps);
        validateUniqueSteps(steps);
    }

    private static void validateUniqueSteps(List<TaskStepDefinition> steps) {
        Set<String> stepCodes = new HashSet<>();
        Set<Integer> stepOrders = new HashSet<>();
        for (TaskStepDefinition step : steps) {
            if (!stepCodes.add(step.stepCode())) {
                throw new IllegalArgumentException("步骤编码重复: " + step.stepCode());
            }
            if (!stepOrders.add(step.stepOrder())) {
                throw new IllegalArgumentException("步骤顺序重复: " + step.stepOrder());
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
