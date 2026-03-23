package com.cdd.api.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ReportHealthResponse(
        @JsonProperty("merchant_id")
        long merchantId,
        @JsonProperty("store_id")
        long storeId,
        boolean ready,
        String summary,
        List<ReportHealthItemResponse> items) {
}
