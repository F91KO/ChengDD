package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ProductPriceSummaryResponse(
        @JsonProperty("min_sale_price")
        BigDecimal minSalePrice,
        @JsonProperty("max_sale_price")
        BigDecimal maxSalePrice) {
}
