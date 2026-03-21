package com.cdd.agent.core.model;

import java.util.Objects;

/**
 * 任务步骤定义模型。
 */
public record TaskStepDefinition(
        String stepCode,
        String stepName,
        int stepOrder,
        String toolCode,
        boolean requiresApproval,
        boolean compensationEnabled) {

    public TaskStepDefinition {
        if (isBlank(stepCode)) {
            throw new IllegalArgumentException("步骤编码不能为空");
        }
        if (isBlank(stepName)) {
            throw new IllegalArgumentException("步骤名称不能为空");
        }
        if (stepOrder <= 0) {
            throw new IllegalArgumentException("步骤顺序必须大于 0");
        }
        Objects.requireNonNull(toolCode, "工具编码不能为空");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
