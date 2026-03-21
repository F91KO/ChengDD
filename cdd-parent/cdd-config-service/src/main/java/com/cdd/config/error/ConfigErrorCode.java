package com.cdd.config.error;

import com.cdd.common.core.error.ErrorCode;

public enum ConfigErrorCode implements ErrorCode {
    CONFIG_NOT_FOUND(40421, "配置项不存在"),
    FEATURE_SWITCH_NOT_FOUND(40422, "功能开关不存在"),
    FEATURE_SWITCH_STATUS_INVALID(40021, "功能开关状态不合法"),
    FEATURE_SWITCH_VALUE_INVALID(40022, "功能开关值不合法");

    private final int code;
    private final String message;

    ConfigErrorCode(int code, String message) {
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
