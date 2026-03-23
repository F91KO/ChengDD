package com.cdd.api.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ReportProductDailyResponse(
        @JsonProperty("id")
        Long id,
        @JsonProperty("merchant_id")
        Long merchantId,
        @JsonProperty("store_id")
        Long storeId,
        @JsonProperty("stat_date")
        String statDate,
        @JsonProperty("product_id")
        Long productId,
        @JsonProperty("sku_id")
        Long skuId,
        @JsonProperty("view_count")
        Long viewCount,
        @JsonProperty("sale_count")
        Long saleCount,
        @JsonProperty("sale_amount")
        BigDecimal saleAmount) {
}
