package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ProductDetailResponse(
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
        @JsonProperty("skus")
        List<ProductSkuResponse> skus) {
}

