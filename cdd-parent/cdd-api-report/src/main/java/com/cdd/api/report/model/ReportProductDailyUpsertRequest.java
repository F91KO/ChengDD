package com.cdd.api.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ReportProductDailyUpsertRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("stat_date")
        @NotBlank(message = "统计日期不能为空")
        String statDate,
        @JsonProperty("product_id")
        @NotNull(message = "商品ID不能为空")
        Long productId,
        @JsonProperty("sku_id")
        Long skuId,
        @JsonProperty("view_count")
        @NotNull(message = "浏览量不能为空")
        Long viewCount,
        @JsonProperty("sale_count")
        @NotNull(message = "销量不能为空")
        Long saleCount,
        @JsonProperty("sale_amount")
        @NotNull(message = "销售额不能为空")
        BigDecimal saleAmount) {
}
