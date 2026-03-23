package com.cdd.api.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportHomeEventDailyUpsertRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("stat_date")
        @NotBlank(message = "统计日期不能为空")
        String statDate,
        @JsonProperty("mini_program_id")
        Long miniProgramId,
        @JsonProperty("page_view_count")
        @NotNull(message = "浏览量不能为空")
        Long pageViewCount,
        @JsonProperty("visitor_count")
        @NotNull(message = "访客数不能为空")
        Long visitorCount,
        @JsonProperty("click_count")
        @NotNull(message = "点击量不能为空")
        Long clickCount) {
}
