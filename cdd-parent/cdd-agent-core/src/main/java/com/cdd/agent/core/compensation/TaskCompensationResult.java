package com.cdd.agent.core.compensation;

import java.util.Objects;

/**
 * 补偿执行结果模型。
 */
public record TaskCompensationResult(TaskCompensationStatus status, String summary) {

    public TaskCompensationResult {
        Objects.requireNonNull(status, "补偿状态不能为空");
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("补偿结果摘要不能为空");
        }
    }
}
