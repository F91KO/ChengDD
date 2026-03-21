package com.cdd.agent.core.audit;

import java.util.Map;

/**
 * 任务审计记录模型。
 */
public record TaskAuditEntry(
        String taskNo,
        String stepCode,
        String actionType,
        String actionResult,
        String operatorType,
        String operatorId,
        String summary,
        Map<String, Object> detail) {

    public TaskAuditEntry {
        if (taskNo == null || taskNo.isBlank()) {
            throw new IllegalArgumentException("审计记录任务编号不能为空");
        }
        if (actionType == null || actionType.isBlank()) {
            throw new IllegalArgumentException("审计动作类型不能为空");
        }
        if (actionResult == null || actionResult.isBlank()) {
            throw new IllegalArgumentException("审计动作结果不能为空");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("审计摘要不能为空");
        }
        detail = detail == null ? Map.of() : Map.copyOf(detail);
    }
}
