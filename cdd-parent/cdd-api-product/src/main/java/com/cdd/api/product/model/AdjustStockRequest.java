package com.cdd.api.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdjustStockRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("product_id")
        @NotNull(message = "商品ID不能为空")
        Long productId,
        @JsonProperty("sku_id")
        @NotNull(message = "SKU ID不能为空")
        Long skuId,
        @JsonProperty("delta_stock")
        @NotNull(message = "库存调整量不能为空")
        Integer deltaStock,
        @JsonProperty("reason")
        @NotBlank(message = "库存变更原因不能为空")
        String reason) {
}

