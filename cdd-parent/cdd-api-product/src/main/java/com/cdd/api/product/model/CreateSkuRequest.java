package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateSkuRequest(
        @JsonProperty("sku_code")
        @NotBlank(message = "SKU编码不能为空")
        String skuCode,
        @JsonProperty("sku_name")
        @NotBlank(message = "SKU名称不能为空")
        String skuName,
        @JsonProperty("sale_price")
        @NotNull(message = "销售价不能为空")
        @DecimalMin(value = "0.00", message = "销售价不能为负数")
        BigDecimal salePrice,
        @JsonProperty("available_stock")
        @NotNull(message = "可售库存不能为空")
        Integer availableStock) {
}

