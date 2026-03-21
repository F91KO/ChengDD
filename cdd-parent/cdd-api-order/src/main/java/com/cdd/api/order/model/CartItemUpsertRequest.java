package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CartItemUpsertRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("store_id")
        @NotNull(message = "店铺ID不能为空")
        Long storeId,
        @JsonProperty("user_id")
        @NotNull(message = "用户ID不能为空")
        Long userId,
        @JsonProperty("product_id")
        @NotNull(message = "商品ID不能为空")
        Long productId,
        @JsonProperty("sku_id")
        @NotNull(message = "SKU ID不能为空")
        Long skuId,
        @Min(value = 1, message = "购买数量必须大于0")
        int quantity,
        @JsonProperty("selected")
        boolean selected,
        @JsonProperty("snapshot_price")
        @NotNull(message = "价格快照不能为空")
        @DecimalMin(value = "0.00", message = "价格快照不能小于0")
        BigDecimal snapshotPrice) {
}
