package com.cdd.report.error;

import com.cdd.common.core.error.ErrorCode;

public enum ReportErrorCode implements ErrorCode {
    REPORT_STAT_DATE_INVALID(40081, "统计日期格式不合法"),
    REPORT_SNAPSHOT_TIME_INVALID(40082, "快照时间格式不合法"),
    REPORT_MERCHANT_DASHBOARD_NOT_FOUND(40481, "商家看板快照不存在"),
    REPORT_PLATFORM_DASHBOARD_NOT_FOUND(40482, "平台看板快照不存在");

    private final int code;
    private final String message;

    ReportErrorCode(int code, String message) {
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
