package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ProductSalesSummaryResponse(
        @JsonProperty("total_sales_quantity")
        int totalSalesQuantity,
        @JsonProperty("total_sales_amount")
        BigDecimal totalSalesAmount) {
}
