package com.cdd.release.error;

import com.cdd.common.core.error.ErrorCode;

public enum ReleaseErrorCode implements ErrorCode {
    RELEASE_TASK_NOT_FOUND(40431, "发布任务不存在"),
    TEMPLATE_VERSION_NOT_FOUND(40432, "模板版本不存在"),
    MINI_PROGRAM_NOT_FOUND(40433, "小程序不存在"),
    RELEASE_STATUS_INVALID(40031, "发布状态不合法"),
    RELEASE_STATUS_TRANSITION_INVALID(40032, "发布状态流转不合法"),
    RELEASE_STEP_STATUS_INVALID(40033, "发布步骤状态不合法"),
    RELEASE_RESULT_SYNC_PRECONDITION_FAILED(40034, "仅成功状态任务允许回写发布结果"),
    RELEASE_MAPPING_STATUS_INVALID(40035, "版本映射状态不合法"),
    RELEASE_ROLLBACK_NOT_ALLOWED(40036, "当前任务状态不允许发起回滚"),
    RELEASE_RESULT_SYNC_FAILED(50031, "发布结果回写失败");

    private final int code;
    private final String message;

    ReleaseErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
