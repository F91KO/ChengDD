package com.cdd.auth.error;

import com.cdd.common.core.error.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
    LOGIN_FAILED(40111, "账号或密码错误"),
    REFRESH_TOKEN_INVALID(40112, "刷新令牌无效或已失效"),
    ACCOUNT_NOT_FOUND(40113, "账号不存在或已失效");

    private final int code;
    private final String message;

    AuthErrorCode(int code, String message) {
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
