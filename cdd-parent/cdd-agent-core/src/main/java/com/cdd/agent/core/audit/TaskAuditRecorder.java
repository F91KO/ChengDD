package com.cdd.agent.core.audit;

/**
 * 任务审计记录接口。
 */
public interface TaskAuditRecorder {

    void record(TaskAuditEntry entry);
}
