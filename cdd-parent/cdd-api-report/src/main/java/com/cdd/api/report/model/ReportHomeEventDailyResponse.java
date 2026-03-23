package com.cdd.api.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReportHomeEventDailyResponse(
        @JsonProperty("id")
        Long id,
        @JsonProperty("merchant_id")
        Long merchantId,
        @JsonProperty("store_id")
        Long storeId,
        @JsonProperty("stat_date")
        String statDate,
        @JsonProperty("mini_program_id")
        Long miniProgramId,
        @JsonProperty("page_view_count")
        Long pageViewCount,
        @JsonProperty("visitor_count")
        Long visitorCount,
        @JsonProperty("click_count")
        Long clickCount) {
}
