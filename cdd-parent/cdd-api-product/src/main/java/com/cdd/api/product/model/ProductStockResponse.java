package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductStockResponse(
        @JsonProperty("product_id")
        long productId,
        @JsonProperty("sku_id")
        long skuId,
        @JsonProperty("available_stock")
        int availableStock,
        @JsonProperty("locked_stock")
        int lockedStock,
        @JsonProperty("stock_status")
        String stockStatus) {
}

