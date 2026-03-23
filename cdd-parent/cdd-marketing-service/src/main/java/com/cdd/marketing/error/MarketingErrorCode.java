package com.cdd.marketing.error;

import com.cdd.common.core.error.ErrorCode;

public enum MarketingErrorCode implements ErrorCode {
    MARKETING_COUPON_STATUS_INVALID(40041, "优惠券状态不合法"),
    MARKETING_ACTIVITY_STATUS_INVALID(40042, "活动状态不合法"),
    MARKETING_RULE_STATUS_INVALID(40043, "推荐规则状态不合法"),
    MARKETING_TOPIC_STATUS_INVALID(40044, "专题会场状态不合法"),
    MARKETING_TOPIC_CODE_DUPLICATE(40941, "专题会场编码重复"),
    MARKETING_ACTIVITY_PRODUCTS_REQUIRED(40045, "活动商品不能为空");

    private final int code;
    private final String message;

    MarketingErrorCode(int code, String message) {
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
