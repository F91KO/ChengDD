package com.cdd.agent.core.approval;

import java.util.Objects;

/**
 * 审批决策模型。
 */
public record TaskApprovalDecision(TaskApprovalStatus status, String reason) {

    public TaskApprovalDecision {
        Objects.requireNonNull(status, "审批状态不能为空");
        if (status == TaskApprovalStatus.REJECTED && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("审批驳回时原因不能为空");
        }
    }
}
