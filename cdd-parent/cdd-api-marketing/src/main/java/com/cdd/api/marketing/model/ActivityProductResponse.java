package com.cdd.api.marketing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ActivityProductResponse(
        @JsonProperty("id")
        Long id,
        @JsonProperty("product_id")
        Long productId,
        @JsonProperty("sku_id")
        Long skuId,
        @JsonProperty("activity_price")
        BigDecimal activityPrice,
        @JsonProperty("sort_order")
        Integer sortOrder) {
}
