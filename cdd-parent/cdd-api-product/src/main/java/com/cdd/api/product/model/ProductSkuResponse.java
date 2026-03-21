package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ProductSkuResponse(
        long id,
        @JsonProperty("product_id")
        long productId,
        @JsonProperty("sku_code")
        String skuCode,
        @JsonProperty("sku_name")
        String skuName,
        @JsonProperty("sale_price")
        BigDecimal salePrice,
        @JsonProperty("available_stock")
        int availableStock,
        @JsonProperty("locked_stock")
        int lockedStock,
        @JsonProperty("stock_status")
        String stockStatus) {
}

