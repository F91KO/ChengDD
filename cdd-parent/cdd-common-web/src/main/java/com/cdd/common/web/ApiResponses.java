package com.cdd.common.web;

import com.cdd.common.core.error.CommonErrorCode;
import com.cdd.common.core.error.ErrorCode;
import com.cdd.common.web.context.RequestIdHolder;

public final class ApiResponses {

    private ApiResponses() {
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.success(data, RequestIdHolder.get());
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return ApiResponse.failure(errorCode, RequestIdHolder.get());
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        return ApiResponse.failure(errorCode.getCode(), message, RequestIdHolder.get());
    }

    public static <T> ApiResponse<T> systemError() {
        return failure(CommonErrorCode.SYSTEM_ERROR);
    }
}
