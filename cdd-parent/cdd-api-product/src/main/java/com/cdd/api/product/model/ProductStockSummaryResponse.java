package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductStockSummaryResponse(
        @JsonProperty("total_available_stock")
        int totalAvailableStock,
        @JsonProperty("total_locked_stock")
        int totalLockedStock,
        @JsonProperty("stock_status")
        String stockStatus) {
}
