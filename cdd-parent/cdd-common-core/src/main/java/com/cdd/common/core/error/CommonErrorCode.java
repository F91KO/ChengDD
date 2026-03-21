package com.cdd.common.core.error;

public enum CommonErrorCode implements ErrorCode {
    SUCCESS(0, "成功"),
    BAD_REQUEST(40001, "参数错误"),
    INVALID_STATE(40003, "当前状态不允许操作"),
    MISSING_REQUIRED_FIELD(40004, "缺少必填字段"),
    VALIDATION_FAILED(40009, "校验未通过"),
    UNAUTHORIZED(40101, "未登录或登录已失效"),
    FORBIDDEN(40301, "无权限访问"),
    NOT_FOUND(40401, "资源不存在"),
    CONFLICT(40901, "数据冲突"),
    SYSTEM_ERROR(50001, "系统异常");

    private final int code;
    private final String message;

    CommonErrorCode(int code, String message) {
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
