package com.cdd.decoration.error;

import com.cdd.common.core.error.ErrorCode;

public enum DecorationErrorCode implements ErrorCode {
    DECORATION_CONFIG_NOT_FOUND(40431, "装修配置不存在"),
    DECORATION_MODULES_REQUIRED(40031, "首页模块不能为空"),
    DECORATION_STYLE_MODE_INVALID(40032, "首页风格不合法"),
    DECORATION_TEMPLATE_CODE_INVALID(40033, "首页模板编码不能为空"),
    DECORATION_VERSION_NOT_FOUND(40432, "装修版本不存在");

    private final int code;
    private final String message;

    DecorationErrorCode(int code, String message) {
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
