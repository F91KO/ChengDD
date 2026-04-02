package com.cdd.merchant.error;

import com.cdd.common.core.error.ErrorCode;

public enum MerchantErrorCode implements ErrorCode {
    APPLICATION_NOT_FOUND(40421, "入驻申请不存在"),
    APPLICATION_STATUS_INVALID(40021, "当前申请状态不允许该操作"),
    REVIEW_DECISION_INVALID(40022, "审核动作不支持"),
    REJECT_REASON_REQUIRED(40023, "驳回时必须填写驳回原因"),
    MINI_PROGRAM_VALIDATION_FAILED(40024, "小程序接入参数校验未通过"),
    MINI_PROGRAM_APP_ID_CONFLICT(40921, "小程序AppID已存在"),
    ONBOARDING_FAILED(50021, "一键开通执行失败"),
    SUB_ACCOUNT_NOT_FOUND(40422, "子账号不存在"),
    SUB_ACCOUNT_SCOPE_INVALID(40025, "数据范围不在当前商家可管理范围内"),
    SUB_ACCOUNT_SCOPE_TYPE_INVALID(40026, "数据范围类型不支持"),
    SUB_ACCOUNT_ROLE_NOT_FOUND(40423, "商家子账号角色不存在");

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
