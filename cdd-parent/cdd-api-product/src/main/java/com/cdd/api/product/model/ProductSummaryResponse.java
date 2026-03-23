package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ProductSummaryResponse(
        long id,
        @JsonProperty("merchant_id")
        long merchantId,
        @JsonProperty("store_id")
        long storeId,
        @JsonProperty("category_id")
        long categoryId,
        @JsonProperty("product_name")
        String productName,
        @JsonProperty("product_sub_title")
        String productSubTitle,
        String status,
        @JsonProperty("sku_count")
        int skuCount,
        @JsonProperty("price_summary")
        ProductPriceSummaryResponse priceSummary,
        @JsonProperty("sales_summary")
        ProductSalesSummaryResponse salesSummary,
        @JsonProperty("stock_summary")
        ProductStockSummaryResponse stockSummary,
        @JsonProperty("sku_summaries")
        List<ProductSkuResponse> skuSummaries) {
}
