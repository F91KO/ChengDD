package com.cdd.agent.core.approval;

import com.cdd.agent.core.context.TaskContext;
import com.cdd.agent.core.model.TaskInstance;

/**
 * 审批网关接口。
 */
public interface TaskApprovalGateway {

    TaskApprovalDecision requestApproval(TaskInstance taskInstance, TaskContext context);
}
