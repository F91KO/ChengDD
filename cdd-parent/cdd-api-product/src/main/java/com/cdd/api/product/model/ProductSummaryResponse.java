package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;

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
        String status,
        @JsonProperty("sku_count")
        int skuCount) {
}

