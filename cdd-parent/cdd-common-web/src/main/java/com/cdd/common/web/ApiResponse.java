package com.cdd.common.web;

import com.cdd.common.core.error.CommonErrorCode;
import com.cdd.common.core.error.ErrorCode;

public class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;
    private final String requestId;

    private ApiResponse(int code, String message, T data, String requestId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.requestId = requestId;
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, null);
    }

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(CommonErrorCode.SUCCESS.getCode(), CommonErrorCode.SUCCESS.getMessage(), data, requestId);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String requestId) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null, requestId);
    }

    public static <T> ApiResponse<T> failure(int code, String message, String requestId) {
        return new ApiResponse<>(code, message, null, requestId);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getRequestId() {
        return requestId;
    }
}
