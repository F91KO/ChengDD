package com.cdd.api.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record CartItemResponse(
        long id,
        @JsonProperty("merchant_id")
        long merchantId,
        @JsonProperty("store_id")
        long storeId,
        @JsonProperty("user_id")
        long userId,
        @JsonProperty("product_id")
        long productId,
        @JsonProperty("sku_id")
        long skuId,
        int quantity,
        boolean selected,
        @JsonProperty("invalid_status")
        String invalidStatus,
        @JsonProperty("snapshot_price")
        BigDecimal snapshotPrice) {
}
