package com.cdd.merchant.error;

import com.cdd.common.core.error.ErrorCode;

public enum MerchantErrorCode implements ErrorCode {
    APPLICATION_NOT_FOUND(40421, "入驻申请不存在"),
    APPLICATION_STATUS_INVALID(40021, "当前申请状态不允许该操作"),
    REVIEW_DECISION_INVALID(40022, "审核动作不支持"),
    REJECT_REASON_REQUIRED(40023, "驳回时必须填写驳回原因"),
    MINI_PROGRAM_VALIDATION_FAILED(40024, "小程序接入参数校验未通过"),
    MINI_PROGRAM_APP_ID_CONFLICT(40921, "小程序AppID已存在"),
    ONBOARDING_FAILED(50021, "一键开通执行失败");

    private final int code;
    private final String message;

    MerchantErrorCode(int code, String message) {
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
